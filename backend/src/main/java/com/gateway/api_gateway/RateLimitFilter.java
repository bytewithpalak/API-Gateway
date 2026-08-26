package com.gateway.api_gateway;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class RateLimitFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ProxyManager<byte[]> proxyManager;
    private final ApiLimitProvider limitProvider;

    public RateLimitFilter(ProxyManager<byte[]> proxyManager, ApiLimitProvider limitProvider) {
        this.proxyManager = proxyManager;
        this.limitProvider = limitProvider;
    }
    private BucketConfiguration buildBucketConfig(ApiLimitConfig limitConfig) {
        Bandwidth bandwidth;

        if (limitConfig.algorithm() == RateLimitAlgorithm.SLIDING_WINDOW) {
            // Sliding window: counts requests within a continuously
            // moving time window, rather than a bucket that refills.
            // Smoother enforcement, no burst allowance.
            bandwidth = Bandwidth.builder()
                    .capacity(limitConfig.capacity())
                    .refillIntervally(limitConfig.capacity(), Duration.ofSeconds(limitConfig.windowSeconds()))
                    .build();
        } else {
            // Token bucket (default): allows a burst up to capacity,
            // then refills gradually.
            bandwidth = Bandwidth.builder()
                    .capacity(limitConfig.capacity())
                    .refillGreedy(limitConfig.capacity(), Duration.ofSeconds(limitConfig.windowSeconds()))
                    .build();
        }

        return BucketConfiguration.builder().addLimit(bandwidth).build();
    }
    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String uri = request.getRequestURI();

        if (uri.startsWith("/test/") || uri.startsWith("/demo/")) {

            // For now the apiId is derived from the URL path prefix, since
            // there's no registration/auth yet to tell us which registered
            // API this request belongs to via an API key. Once that exists,
            // this should come from the caller's API key instead.
            String apiId = uri.startsWith("/demo/") ? "demo-api" : "test-api";

            String clientId = request.getRemoteAddr();
            byte[] key = (apiId + ":" + clientId).getBytes(StandardCharsets.UTF_8);

            // Look up this API's configured limit instead of using a
            // hardcoded constant. Today this comes from InMemoryApiLimitProvider;
            // later it will come from the real registration database.
            ApiLimitConfig limitConfig = limitProvider.getLimitFor(apiId);

            try {
                Bucket bucket = proxyManager.builder().build(key, () -> buildBucketConfig(limitConfig));

                if (!bucket.tryConsume(1)) {
                    response.setStatus(429);
                    response.setHeader("Retry-After", String.valueOf(limitConfig.windowSeconds()));
                    response.getWriter().write("Rate limit exceeded. Try again later.");
                    return;
                }
            } catch (Exception e) {
                log.warn("Rate limiter backend (Redis) unreachable — failing open and allowing request through. Reason: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}