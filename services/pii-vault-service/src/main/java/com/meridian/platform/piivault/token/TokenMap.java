package com.meridian.platform.piivault.token;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "TOKEN_MAP")
public class TokenMap {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tokenMapSeq")
    @SequenceGenerator(name = "tokenMapSeq", sequenceName = "TOKEN_MAP_SEQ", allocationSize = 50)
    @Column(name = "TOKEN_MAP_ID")
    private Long id;

    @Column(name = "TOKEN", nullable = false, length = 64)
    private String token;

    @Column(name = "PII_TYPE", nullable = false, length = 16)
    private String piiType;

    @Column(name = "KEY_VERSION", nullable = false)
    private int keyVersion;

    @Column(name = "CIPHERTEXT", nullable = false, length = 512)
    private String ciphertext;

    @Column(name = "VALUE_HASH", nullable = false, length = 64)
    private String valueHash;

    @Column(name = "CREATED_BY", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPiiType() { return piiType; }
    public void setPiiType(String piiType) { this.piiType = piiType; }
    public int getKeyVersion() { return keyVersion; }
    public void setKeyVersion(int keyVersion) { this.keyVersion = keyVersion; }
    public String getCiphertext() { return ciphertext; }
    public void setCiphertext(String ciphertext) { this.ciphertext = ciphertext; }
    public String getValueHash() { return valueHash; }
    public void setValueHash(String valueHash) { this.valueHash = valueHash; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
