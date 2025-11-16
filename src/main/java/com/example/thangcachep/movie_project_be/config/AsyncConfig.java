package com.example.thangcachep.movie_project_be.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async Configuration
 * Cấu hình thread pool cho @Async methods
 *
 * Sử dụng:
 * - Email sending (không block HTTP request)
 * - File deletion (không block HTTP request)
 * - Background tasks
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread pool cho email tasks
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 2 threads cố định
        executor.setMaxPoolSize(5); // Tối đa 5 threads
        executor.setQueueCapacity(100); // Queue 100 tasks
        executor.setThreadNamePrefix("email-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool cho file operations (delete, process)
     */
    @Bean(name = "fileExecutor")
    public Executor fileExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3); // 3 threads cố định
        executor.setMaxPoolSize(10); // Tối đa 10 threads
        executor.setQueueCapacity(200); // Queue 200 tasks
        executor.setThreadNamePrefix("file-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool cho general background tasks
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 5 threads cố định
        executor.setMaxPoolSize(20); // Tối đa 20 threads
        executor.setQueueCapacity(500); // Queue 500 tasks
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

