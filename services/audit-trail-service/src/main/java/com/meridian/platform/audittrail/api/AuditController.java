package com.meridian.platform.audittrail.api;

import com.meridian.platform.audittrail.audit.AuditEvent;
import com.meridian.platform.audittrail.audit.AuditTrailService;
import com.meridian.platform.audittrail.ingest.AuditEventMessage;
import com.meridian.platform.audittrail.ingest.EventMapper;
import com.meridian.platform.common.correlation.CorrelationId;
import com.meridian.platform.common.error.ApiException;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit/v1")
public class AuditController {

    private final AuditTrailService service;
    private final EventMapper mapper;

    public AuditController(AuditTrailService service, EventMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** Synchronous ingest for callers that cannot reach the broker (the BFFs, documents-service). */
    @PostMapping("/events")
    public ResponseEntity<AuditEvent> ingest(@Valid @RequestBody AuditEventMessage body) {
        AuditEvent e = mapper.fromMessage(body);
        if (e.getCorrelationId() == null) {
            e.setCorrelationId(CorrelationId.current());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.append(e));
    }

    @GetMapping("/subjects/{subjectType}/{subjectId}")
    public List<AuditEvent> bySubject(@PathVariable String subjectType, @PathVariable String subjectId) {
        return service.forSubject(subjectType.toUpperCase(), subjectId);
    }

    @GetMapping("/events")
    public List<AuditEvent> query(@RequestParam(required = false) String actor,
                                  @RequestParam(required = false) String correlationId) {
        if (actor != null) {
            return service.forActor(actor);
        }
        if (correlationId != null) {
            return service.forCorrelation(correlationId);
        }
        throw ApiException.badRequest("AUDIT_FILTER", "actor or correlationId is required");
    }

    @GetMapping("/verify")
    public Map<String, Object> verify() {
        return service.verify();
    }
}
