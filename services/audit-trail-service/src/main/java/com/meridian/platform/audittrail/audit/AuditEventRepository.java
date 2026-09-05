package com.meridian.platform.audittrail.audit;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Optional<AuditEvent> findTopByOrderByIdDesc();

    List<AuditEvent> findTop500BySubjectTypeAndSubjectIdOrderByEventTimeDesc(String subjectType, String subjectId);

    List<AuditEvent> findTop500ByActorOrderByEventTimeDesc(String actor);

    List<AuditEvent> findByCorrelationIdOrderByEventTimeAsc(String correlationId);

    List<AuditEvent> findAllByOrderByIdAsc();

    boolean existsBySourceTopicAndSourceOffset(String sourceTopic, long sourceOffset);
}
