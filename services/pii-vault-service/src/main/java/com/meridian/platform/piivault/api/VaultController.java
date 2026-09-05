package com.meridian.platform.piivault.api;

import com.meridian.platform.common.error.ApiException;
import com.meridian.platform.piivault.access.PiiAccess;
import com.meridian.platform.piivault.access.PiiAccessRepository;
import com.meridian.platform.piivault.token.PiiType;
import com.meridian.platform.piivault.token.TokenService;
import com.meridian.platform.piivault.token.TokenService.Caller;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vault/v1")
public class VaultController {

    private final TokenService service;
    private final PiiAccessRepository accessLog;

    public VaultController(TokenService service, PiiAccessRepository accessLog) {
        this.service = service;
        this.accessLog = accessLog;
    }

    @PostMapping("/tokens")
    public Map<String, Object> tokenise(@Valid @RequestBody TokenRequest body,
                                        @RequestHeader(value = "X-Purpose", required = false) String purpose,
                                        @RequestHeader(value = "X-Calling-Service", required = false) String callingService,
                                        Principal principal) {
        PiiType type = parseType(body.type);
        String token = service.tokenise(type, body.value, caller(principal, callingService, purpose));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", type.name());
        out.put("token", token);
        out.put("masked", TokenService.mask(body.value, type));
        return out;
    }

    @PostMapping("/detokenise")
    public Map<String, Object> detokenise(@Valid @RequestBody TokenRequest body,
                                          @RequestHeader(value = "X-Purpose", required = false) String purpose,
                                          @RequestHeader(value = "X-Calling-Service", required = false) String callingService,
                                          Principal principal) {
        PiiType type = parseType(body.type);
        String value = service.detokenise(type, body.value, caller(principal, callingService, purpose));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", type.name());
        out.put("value", value);
        return out;
    }

    @GetMapping("/access-log")
    public List<PiiAccess> accessLog(@RequestParam(required = false) String token,
                                     @RequestParam(required = false) String principal) {
        if (token != null) {
            return accessLog.findTop200ByTokenOrderByAccessedAtDesc(token);
        }
        if (principal != null) {
            return accessLog.findTop200ByPrincipalOrderByAccessedAtDesc(principal);
        }
        throw ApiException.badRequest("ACCESS_LOG_FILTER", "token or principal is required");
    }

    private static PiiType parseType(String s) {
        try {
            return PiiType.parse(s);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("PII_TYPE", "unknown pii type " + s);
        }
    }

    private static Caller caller(Principal principal, String service, String purpose) {
        return new Caller(principal == null ? null : principal.getName(), service, purpose);
    }

    public static class TokenRequest {
        @NotBlank
        public String type;
        @NotBlank
        @Size(max = 64)
        public String value;
    }
}
