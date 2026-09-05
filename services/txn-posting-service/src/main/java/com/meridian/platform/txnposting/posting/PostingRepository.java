package com.meridian.platform.txnposting.posting;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    Optional<Posting> findByIdempotencyKey(String idempotencyKey);

    List<Posting> findTop50ByAccountIdOrderByCreatedAtDesc(String accountId);

    List<Posting> findByStatusOrderByCreatedAtAsc(String status);

    @Query("select coalesce(sum(p.amountMinor), 0) from Posting p where p.accountId = :accountId "
        + "and p.type = 'DEBIT' and p.status in ('POSTED', 'PENDING_BEDROCK') and p.createdAt >= :since")
    long debitedSince(@Param("accountId") String accountId, @Param("since") Instant since);
}
