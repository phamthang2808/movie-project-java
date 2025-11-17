package com.example.thangcachep.movie_project_be.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;

/**
 * Redis Configuration
 *
 * Tự động detect Redis:
 * - Nếu spring.data.redis.enabled=true VÀ Redis server đang chạy → dùng Redis cache
 * - Nếu Redis server không chạy → CacheConfig sẽ load với NoOpCacheManager (fallback)
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {

    /**
     * ObjectMapper với JSR310Module để serialize LocalDate, LocalDateTime
     * Và hỗ trợ deserialize cho class có @Builder, @AllArgsConstructor, @NoArgsConstructor
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Cấu hình để hỗ trợ deserialize vào class có @Builder
        // Sử dụng @AllArgsConstructor và @NoArgsConstructor để deserialize
        mapper.setVisibility(
                com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
                com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
        );
        mapper.setVisibility(
                com.fasterxml.jackson.annotation.PropertyAccessor.CREATOR,
                com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
        );

        // Cho phép deserialize vào constructor hoặc builder
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

        return mapper;
    }

    /**
     * RedisConnectionFactory - tự tạo để tránh lỗi khi Redis không chạy
     * Chỉ tạo khi enabled=true và Redis server đang chạy
     * Hỗ trợ TLS cho Upstash Redis
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.ssl:false}") boolean ssl) {

        // Cleanup host: remove protocol nếu có (http://, https://, rediss://, redis://)
        String cleanHost = host;
        if (cleanHost != null) {
            cleanHost = cleanHost.replaceFirst("^(rediss?|https?)://", "");
            // Remove username:password@ nếu có
            if (cleanHost.contains("@")) {
                cleanHost = cleanHost.substring(cleanHost.indexOf("@") + 1);
            }
        }

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(cleanHost);
        config.setPort(port);
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }

        // Cấu hình TLS/SSL cho Upstash Redis
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(10)); // Timeout 10 giây

        // Tự động bật SSL nếu host chứa "upstash.io" hoặc ssl=true
        boolean useSsl = ssl || (cleanHost != null && cleanHost.contains("upstash.io"));
        if (useSsl) {
            builder.useSsl()
                    .and()
                    .clientOptions(ClientOptions.builder()
                            .sslOptions(SslOptions.builder()
                                    .build())
                            .build());
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, builder.build());
        factory.setValidateConnection(true); // Validate connection khi tạo
        return factory;
    }

    /**
     * RedisTemplate để thao tác với Redis trực tiếp
     * Chỉ tạo khi Redis được bật (spring.data.redis.enabled=true)
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key serializer: String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serializer: JSON với ObjectMapper hỗ trợ LocalDate
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(createObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager với Redis - chỉ tạo khi Redis được bật
     */
    @Bean(name = "cacheManager")
    @Primary
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // ObjectMapper với JSR310Module cho cache
        ObjectMapper cacheObjectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        // Default cache config: 1 giờ
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues(); // Không cache null values

        // Movies cache: 2 giờ (dữ liệu ít thay đổi)
        RedisCacheConfiguration moviesConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // Categories cache: 6 giờ (rất ít thay đổi)
        RedisCacheConfiguration categoriesConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // Top movies cache: 30 phút (thay đổi thường xuyên hơn)
        RedisCacheConfiguration topMoviesConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // Users cache: 15 phút (admin/staff pages)
        RedisCacheConfiguration usersConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // Comments cache: 10 phút (admin/staff pages)
        RedisCacheConfiguration commentsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("movies", moviesConfig)
                .withCacheConfiguration("categories", categoriesConfig)
                .withCacheConfiguration("topMovies", topMoviesConfig)
                .withCacheConfiguration("users", usersConfig)
                .withCacheConfiguration("comments", commentsConfig)
                .build();
    }

}

