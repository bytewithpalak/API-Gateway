package com.gateway.api_gateway.service;

import com.gateway.api_gateway.entity.RequestLog;
import com.gateway.api_gateway.repository.RequestLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// Called by Person 1's gateway/proxy code AFTER a request has already been
// allowed/blocked and (if allowed) relayed - never before, and never awaited
// on the critical path. @Async hands the DB write to a separate thread pool
// so a slow Postgres write can't add to the client-facing latency, which is
// the "Added latency (p50/p95)" metric's whole point.
//
// Trade-off worth documenting for the report: @Async is fire-and-forget, so
// if the app crashes between "response sent" and "row written", that request
// is missing from the log. That's the direct risk to the >=99.9% log
// completeness target - flag it, and if time allows, swap this for a
// message queue (e.g. a lightweight outbox table) for stronger guarantees.
@Service
public class RequestLogService {

    private final RequestLogRepository requestLogRepository;

    public RequestLogService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    @Async
    public void logAsync(Long apiId, String clientIdentity, RequestLog.Outcome outcome, Long latencyMs) {
        RequestLog log = new RequestLog(apiId, clientIdentity, outcome, latencyMs);
        requestLogRepository.save(log);
    }
}
