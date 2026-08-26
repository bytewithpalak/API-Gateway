package com.gateway.api_gateway;

/**
 * Holds a rate-limit rule: how many requests (capacity) are allowed
 * per window (windowSeconds), and which algorithm to enforce it with.
 */
public record ApiLimitConfig(int capacity, int windowSeconds, RateLimitAlgorithm algorithm) {
}