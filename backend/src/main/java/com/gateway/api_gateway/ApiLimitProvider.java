package com.gateway.api_gateway;

/**
 * Resolves the rate-limit configuration for a given registered API.
 *
 * This is a seam: today it's backed by an in-memory placeholder
 * (InMemoryApiLimitProvider). Once the persistence/auth workstream
 * has a real registration table (apiId -> limit, windowSeconds),
 * a new implementation of this interface can read from Postgres
 * instead, with no changes needed to RateLimitFilter.
 */
public interface ApiLimitProvider {

    /**
     * @param apiId identifier of the registered API (for now, just a
     *              hardcoded string like "test-api"; later this will
     *              come from the caller's API key)
     * @return the limit configuration to apply, or a sensible default
     *         if the apiId isn't recognised
     */
    ApiLimitConfig getLimitFor(String apiId);
}