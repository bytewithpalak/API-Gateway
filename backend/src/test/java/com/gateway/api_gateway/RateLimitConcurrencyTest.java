package com.gateway.api_gateway;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.jedis.Bucket4jJedis;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class RateLimitConcurrencyTest {

    // Spins up a real, throwaway Redis container just for this test
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    @Test
    void bucketAdmitsOnlyConfiguredLimitUnderConcurrentLoad() throws InterruptedException {

        // Connect to whatever port Testcontainers actually mapped 6379 to
        JedisPool jedisPool = new JedisPool(redis.getHost(), redis.getMappedPort(6379));
        ProxyManager<byte[]> proxyManager = Bucket4jJedis.casBasedBuilder(jedisPool).build();

        int limit = 20; // N
        int totalRequests = 200; // M, much greater than N

        byte[] key = "concurrency-test-key".getBytes(StandardCharsets.UTF_8);

        BucketConfiguration config = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit)
                        .refillGreedy(limit, Duration.ofMinutes(10)) // long window, no refill mid-test
                        .build())
                .build();

        Bucket bucket = proxyManager.builder().build(key, () -> config);

        AtomicInteger admitted = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            pool.submit(() -> {
                try {
                    if (bucket.tryConsume(1)) {
                        admitted.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // wait for all threads to finish
        pool.shutdown();

        System.out.println("Admitted: " + admitted.get() + " out of " + totalRequests + " (limit was " + limit + ")");

        // Primary metric from proposal §6.1: admitted count within ~2% of limit
        double tolerance = limit * 0.02;
        int allowedDeviation = Math.max((int) Math.ceil(tolerance), 1);
        assertTrue(Math.abs(admitted.get() - limit) <= allowedDeviation,
                "Expected admitted count close to " + limit + " but got " + admitted.get());
    }
}