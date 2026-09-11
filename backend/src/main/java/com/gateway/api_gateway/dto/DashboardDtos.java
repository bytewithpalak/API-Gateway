package com.gateway.api_gateway.dto;

import java.time.Instant;

public class DashboardDtos {

    public record SummaryResponse(
        Long apiId,
        long allowed,
        long blocked,
        Instant from,
        Instant to
    ) {}

    public record LogEntryResponse(
        Long id,
        String clientIdentity,
        String outcome,
        Instant timestamp,
        Long latencyMs
    ) {}

    public record CompletenessResponse(
        Long apiId,
        long expectedCount,
        long actualCount,
        double completenessRatio,
        boolean meetsTarget // true once completenessRatio >= 0.999
    ) {}
}
