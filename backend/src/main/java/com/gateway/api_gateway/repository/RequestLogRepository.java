package com.gateway.api_gateway.repository;

import com.gateway.api_gateway.entity.RequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    // Paginated raw log view, filterable by API - backs "request log with
    // filtering and pagination" from the subsequent-deliverables list.
    Page<RequestLog> findByApiIdOrderByTimestampDesc(Long apiId, Pageable pageable);

    // Full filtered view: outcome, client identity (partial match) and a
    // date range are all optional - pass null for any filter you don't want
    // applied. This is what closes the "filtering" gap on the dashboard
    // logs endpoint (previously apiId + pagination only).
    @Query("""
        SELECT r FROM RequestLog r
        WHERE r.apiId = :apiId
          AND (:outcome IS NULL OR r.outcome = :outcome)
          AND (:clientIdentity IS NULL OR r.clientIdentity LIKE %:clientIdentity%)
          AND (:from IS NULL OR r.timestamp >= :from)
          AND (:to IS NULL OR r.timestamp <= :to)
        ORDER BY r.timestamp DESC
        """)
    Page<RequestLog> findFiltered(@Param("apiId") Long apiId,
                                   @Param("outcome") RequestLog.Outcome outcome,
                                   @Param("clientIdentity") String clientIdentity,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to,
                                   Pageable pageable);

    // Total row count for one API in a window, independent of outcome -
    // used as the "actually logged" side of the completeness ratio.
    long countByApiIdAndTimestampBetween(Long apiId, Instant from, Instant to);

    // Aggregated allowed-vs-blocked counts per API, for the dashboard summary card.
    // Pre-aggregating server-side keeps raw logs off the wire (operational constraint
    // from the proposal: don't ship raw logs to the browser).
    @Query("""
        SELECT r.outcome as outcome, COUNT(r) as count
        FROM RequestLog r
        WHERE r.apiId = :apiId AND r.timestamp BETWEEN :from AND :to
        GROUP BY r.outcome
        """)
    List<OutcomeCount> countByOutcome(@Param("apiId") Long apiId,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);

    interface OutcomeCount {
        RequestLog.Outcome getOutcome();
        long getCount();
    }
}
