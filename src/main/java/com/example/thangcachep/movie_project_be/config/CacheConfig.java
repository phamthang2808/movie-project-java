package com.example.thangcachep.movie_project_be.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Cache Configuration fallback khi Redis không có sẵn
 * Sử dụng NoOpCacheManager - không cache, mọi request đều query database trực tiếp
 *
 * Load khi:
 * - spring.data.redis.enabled=false (force disable)
 * - HOẶC RedisConnectionFactory không tồn tại (Redis server không chạy hoặc không kết nối được)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Fallback CacheManager khi Redis không có sẵn
     * NoOpCacheManager sẽ không cache gì cả, mọi request đều query database trực tiếp
     * Điều này đảm bảo ứng dụng vẫn chạy được khi không có Redis
     *
     * Chỉ tạo khi:
     * - spring.data.redis.enabled=false (force disable)
     * - HOẶC không có RedisConnectionFactory (Redis server không chạy hoặc không kết nối được)
     */
    @Bean(name = "cacheManager")
    @Primary
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager fallbackCacheManager() {
        // Load khi: enabled=false HOẶC không set property (mặc định) HOẶC không có RedisConnectionFactory
        return new NoOpCacheManager();
    }
}

