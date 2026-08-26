package com.gateway.api_gateway;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.jedis.Bucket4jJedis;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class RedisConfig {

    @Bean
    public JedisPool jedisPool() {
        // Connects to Redis running on localhost:6379 (the Docker container from Step 1)
        return new JedisPool("localhost", 6379);
    }

    @Bean
    public ProxyManager<byte[]> proxyManager(JedisPool jedisPool) {
        return Bucket4jJedis.casBasedBuilder(jedisPool)
                .build();
    }
}