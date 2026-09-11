package com.gateway.api_gateway.controller;

import com.gateway.api_gateway.dto.DashboardDtos.CompletenessResponse;
import com.gateway.api_gateway.dto.DashboardDtos.LogEntryResponse;
import com.gateway.api_gateway.dto.DashboardDtos.SummaryResponse;
import com.gateway.api_gateway.entity.RequestLog;
import com.gateway.api_gateway.repository.RequestLogRepository;
import com.gateway.api_gateway.service.LogCompletenessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Everything Person 3's React dashboard calls. Deliberately returns
// pre-aggregated summaries and paginated slices, never the full log table,
// per the proposal's "server-side aggregation rather than shipping raw logs
// to the browser" constraint.
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final RequestLogRepository requestLogRepository;
    private final LogCompletenessService logCompletenessService;

    public DashboardController(RequestLogRepository requestLogRepository,
                                LogCompletenessService logCompletenessService) {
        this.requestLogRepository = requestLogRepository;
        this.logCompletenessService = logCompletenessService;
    }

    @GetMapping("/{apiId}/summary")
    public ResponseEntity<SummaryResponse> summary(
        @PathVariable Long apiId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(24, ChronoUnit.HOURS);

        long allowed = 0;
        long blocked = 0;
        for (var row : requestLogRepository.countByOutcome(apiId, effectiveFrom, effectiveTo)) {
            if (row.getOutcome() == RequestLog.Outcome.ALLOWED) allowed = row.getCount();
            else blocked = row.getCount();
        }

        return ResponseEntity.ok(new SummaryResponse(apiId, allowed, blocked, effectiveFrom, effectiveTo));
    }

    // All filters are optional - omit any of them to fall back to "no
    // restriction" on that field. This is what the dashboard's log table
    // filter controls (outcome dropdown, client search box, date pickers)
    // will call.
    @GetMapping("/{apiId}/logs")
    public ResponseEntity<Page<LogEntryResponse>> logs(
        @PathVariable Long apiId,
        @RequestParam(required = false) RequestLog.Outcome outcome,
        @RequestParam(required = false) String clientIdentity,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        Page<LogEntryResponse> result = requestLogRepository
            .findFiltered(apiId, outcome, clientIdentity, from, to, PageRequest.of(page, size))
            .map(log -> new LogEntryResponse(
                log.getId(),
                log.getClientIdentity(),
                log.getOutcome().name(),
                log.getTimestamp(),
                log.getLatencyMs()
            ));
        return ResponseEntity.ok(result);
    }

    // Reports the proposal's log-completeness metric for one API over a
    // window. "expected" has to come from whoever drove the load (a
    // load-test script's own request tally, or a counter kept by the
    // gateway) - this endpoint can only tell you what fraction of that
    // actually landed in Postgres.
    @GetMapping("/{apiId}/completeness")
    public ResponseEntity<CompletenessResponse> completeness(
        @PathVariable Long apiId,
        @RequestParam long expected,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return ResponseEntity.ok(logCompletenessService.measure(apiId, expected, from, to));
    }
}
