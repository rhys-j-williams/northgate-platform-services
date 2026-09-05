package com.meridian.platform.piivault.token;

import com.meridian.platform.common.error.ApiException;
import com.meridian.platform.piivault.access.AccessLogger;
import com.meridian.platform.piivault.access.PurposeCode;
import com.meridian.platform.piivault.access.PurposeCodeRepository;
import com.meridian.platform.piivault.keys.KeyMaterial;
import com.meridian.platform.piivault.keys.VaultKeyClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenMapRepository tokens;
    private final PurposeCodeRepository purposes;
    private final VaultKeyClient keys;
    private final AccessLogger access;

    public TokenService(TokenMapRepository tokens, PurposeCodeRepository purposes, VaultKeyClient keys, AccessLogger access) {
        this.tokens = tokens;
        this.purposes = purposes;
        this.keys = keys;
        this.access = access;
    }

    @Transactional
    public String tokenise(PiiType type, String value, Caller caller) {
        requirePurpose(caller.purpose, false, "TOKENISE", type, null, caller);
        KeyMaterial key = keys.current();
        String token;
        try {
            token = new FormatPreservingCipher(key.getKey()).encrypt(value, type);
        } catch (IllegalArgumentException e) {
            access.record("TOKENISE", type.name(), null, caller.principal, caller.service, caller.purpose, "REJECTED");
            throw ApiException.badRequest("PII_FORMAT", e.getMessage());
        }
        String hash = sha256(type.name() + ":" + value);
        Optional<TokenMap> existing = tokens.findByToken(token);
        if (!existing.isPresent()) {
            TokenMap row = new TokenMap();
            row.setToken(token);
            row.setPiiType(type.name());
            row.setKeyVersion(key.getVersion());
            row.setCiphertext(seal(key, value));
            row.setValueHash(hash);
            row.setCreatedBy(caller.principal == null ? "anonymous" : caller.principal);
            tokens.save(row);
        }
        access.record("TOKENISE", type.name(), token, caller.principal, caller.service, caller.purpose, "OK");
        return token;
    }

    @Transactional
    public String detokenise(PiiType type, String token, Caller caller) {
        requirePurpose(caller.purpose, true, "DETOKENISE", type, token, caller);
        TokenMap row = tokens.findByToken(token).orElse(null);
        String plain;
        if (row != null) {
            plain = open(keys.forVersion(row.getKeyVersion()), row.getCiphertext());
        } else {
            // No map row: token was minted before the map existed (pre-2021) or by another region.
            // FPE is reversible, so decrypt with the current key and hope. Flagged in the access log.
            plain = new FormatPreservingCipher(keys.current().getKey()).decrypt(token, type);
        }
        access.record("DETOKENISE", type.name(), token, caller.principal, caller.service, caller.purpose,
            row == null ? "OK_UNMAPPED" : "OK");
        return plain;
    }

    public static String mask(String value, PiiType type) {
        int keep = Math.max(type.preserveTail(), 4);
        StringBuilder sb = new StringBuilder(value.length());
        int digitsSeen = 0;
        int totalDigits = 0;
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c)) {
                totalDigits++;
            }
        }
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c)) {
                digitsSeen++;
                sb.append(digitsSeen > totalDigits - keep ? c : '*');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void requirePurpose(String purpose, boolean detokenise, String op, PiiType type, String token, Caller caller) {
        PurposeCode code = purpose == null ? null : purposes.findById(purpose).orElse(null);
        if (code == null) {
            access.record(op, type.name(), token, caller.principal, caller.service, purpose, "REFUSED");
            throw ApiException.forbidden("PURPOSE_UNKNOWN", "X-Purpose header missing or not an approved purpose code");
        }
        if (detokenise && !code.isAllowsDetokenise()) {
            access.record(op, type.name(), token, caller.principal, caller.service, purpose, "REFUSED");
            throw ApiException.forbidden("PURPOSE_TOKENISE_ONLY", "purpose " + purpose + " does not permit detokenise");
        }
    }

    private static String seal(KeyMaterial key, String plain) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getKey(), 0, 16, "AES"), new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String open(KeyMaterial key, String sealed) {
        try {
            byte[] in = Base64.getDecoder().decode(sealed);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getKey(), 0, 16, "AES"), new GCMParameterSpec(128, in, 0, 12));
            return new String(c.doFinal(in, 12, in.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw ApiException.conflict("PII_KEY_MISMATCH", "stored value cannot be opened with key v" + key.getVersion());
        }
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static final class Caller {
        final String principal;
        final String service;
        final String purpose;

        public Caller(String principal, String service, String purpose) {
            this.principal = principal;
            this.service = service;
            this.purpose = purpose;
        }
    }
}
