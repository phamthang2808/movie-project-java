package com.example.thangcachep.movie_project_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application
 *
 * Redis mặc định TẮT (không dùng Redis):
 * - Mặc định: Dùng database (NoOpCacheManager), không lỗi
 * - Để BẬT Redis: Sửa application.yml → spring.data.redis.enabled: true
 */
@SpringBootApplication(exclude = {
        // Exclude RedisAutoConfiguration để tránh lỗi khi Redis không chạy
        // RedisConfig sẽ tự tạo RedisConnectionFactory khi Redis server đang chạy
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
@EnableScheduling  // Enable scheduled tasks (VIP expiration check)
public class MovieProjectBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieProjectBeApplication.class, args);
    }

}
