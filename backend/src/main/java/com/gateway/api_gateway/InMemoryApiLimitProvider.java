package com.gateway.api_gateway;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Temporary stand-in for ApiLimitProvider, backed by a hardcoded map
 * instead of a real database. See ApiLimitProvider for the full
 * explanation of why this exists and what should eventually replace it.
 */
@Component
public class InMemoryApiLimitProvider implements ApiLimitProvider {

    private static final ApiLimitConfig DEFAULT =
            new ApiLimitConfig(5, 20, RateLimitAlgorithm.TOKEN_BUCKET);

    // Two example "registered APIs" - one using each algorithm, so both
    // code paths are exercisable for testing/demo purposes.
    private final Map<String, ApiLimitConfig> limits = Map.of(
            "test-api", new ApiLimitConfig(5, 20, RateLimitAlgorithm.TOKEN_BUCKET),
            "demo-api", new ApiLimitConfig(5, 20, RateLimitAlgorithm.SLIDING_WINDOW)
    );

    @Override
    public ApiLimitConfig getLimitFor(String apiId) {
        return limits.getOrDefault(apiId, DEFAULT);
    }
}