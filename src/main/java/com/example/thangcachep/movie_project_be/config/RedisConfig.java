package com.example.thangcachep.movie_project_be.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableCaching
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
     * RedisTemplate để thao tác với Redis trực tiếp
     */
    @Bean
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
     * CacheManager để quản lý cache với Spring Cache abstraction
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
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

