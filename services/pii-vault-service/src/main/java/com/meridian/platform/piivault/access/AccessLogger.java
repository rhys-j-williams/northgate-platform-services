package com.meridian.platform.piivault.access;

import com.meridian.platform.common.correlation.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the access row in its own transaction so a refused detokenise (which rolls back nothing
 * else anyway) still leaves evidence. Also emits a structured log line; Splunk alerts on
 * outcome=REFUSED from the same principal more than five times in a minute (GIS-0331).
 */
@Component
public class AccessLogger {

    private static final Logger audit = LoggerFactory.getLogger("com.meridian.platform.piivault.AUDIT");

    private final PiiAccessRepository repository;

    public AccessLogger(PiiAccessRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String operation, String piiType, String token, String principal, String callingService,
                       String purpose, String outcome) {
        PiiAccess row = new PiiAccess();
        row.setOperation(operation);
        row.setPiiType(piiType);
        row.setToken(token);
        row.setPrincipal(principal == null ? "anonymous" : principal);
        row.setCallingService(callingService);
        row.setPurpose(purpose == null ? "UNSPECIFIED" : purpose);
        row.setOutcome(outcome);
        row.setCorrelationId(CorrelationId.current());
        repository.save(row);
        audit.info("pii_access operation={} type={} principal={} service={} purpose={} outcome={}",
            operation, piiType, row.getPrincipal(), callingService, row.getPurpose(), outcome);
    }
}
