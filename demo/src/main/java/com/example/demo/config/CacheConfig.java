package com.example.demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for tax rates.
 * Tax rates change infrequently (typically once per year), 
 * so caching provides significant performance benefits.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache manager with Caffeine.
     * Tax rates cached for 1 hour (3600 seconds).
     * Max 500 entries (290 municipalities * ~4 tax types = ~1160 possible, but most won't be accessed).
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("taxRates", "municipalities");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(500)
                .recordStats());
        return cacheManager;
    }
}
