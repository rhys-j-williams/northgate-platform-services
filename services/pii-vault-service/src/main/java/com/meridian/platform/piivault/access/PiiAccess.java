package com.meridian.platform.piivault.access;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/** One row per tokenise/detokenise attempt, including refused ones. Never updated. */
@Entity
@Table(name = "PII_ACCESS")
public class PiiAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "piiAccessSeq")
    @SequenceGenerator(name = "piiAccessSeq", sequenceName = "PII_ACCESS_SEQ", allocationSize = 100)
    @Column(name = "ACCESS_ID")
    private Long id;

    @Column(name = "OPERATION", nullable = false, length = 12)
    private String operation;

    @Column(name = "PII_TYPE", nullable = false, length = 16)
    private String piiType;

    @Column(name = "TOKEN", length = 64)
    private String token;

    @Column(name = "PRINCIPAL", nullable = false, length = 64)
    private String principal;

    @Column(name = "CALLING_SERVICE", length = 64)
    private String callingService;

    @Column(name = "PURPOSE", nullable = false, length = 40)
    private String purpose;

    @Column(name = "OUTCOME", nullable = false, length = 12)
    private String outcome;

    @Column(name = "CORRELATION_ID", length = 64)
    private String correlationId;

    @Column(name = "ACCESSED_AT", nullable = false)
    private Instant accessedAt = Instant.now();

    public Long getId() { return id; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getPiiType() { return piiType; }
    public void setPiiType(String piiType) { this.piiType = piiType; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPrincipal() { return principal; }
    public void setPrincipal(String principal) { this.principal = principal; }
    public String getCallingService() { return callingService; }
    public void setCallingService(String callingService) { this.callingService = callingService; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getAccessedAt() { return accessedAt; }
}
