package com.example.orderservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    // Cache name constants – used by @Cacheable annotations
    public static final String CACHE_ORDER_BY_ID        = "order:id";
    public static final String CACHE_ORDER_BY_REFERENCE = "order:ref";
    public static final String CACHE_ORDER_LIST_CUSTOMER= "order:list:customer";

    @Value("${spring.cache.redis.time-to-live:PT30M}")         // 30 min default
    private Duration defaultTtl;

    @Value("${cache.order.ttl:PT30M}")
    private Duration orderTtl;

    @Value("${cache.order-list.ttl:PT5M}")
    private Duration orderListTtl;

    // ─── ObjectMapper tuned for Redis JSON ────────────────────────────────────

    @Bean
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfSubType(Object.class).build(),
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );
    }

    // ─── Generic RedisTemplate<String, Object> ────────────────────────────────

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper redisObjectMapper) {

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    // ─── Spring CacheManager with per-cache TTLs ──────────────────────────────

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory,
                                     ObjectMapper redisObjectMapper) {

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(defaultTtl)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_ORDER_BY_ID,
                defaultConfig.entryTtl(orderTtl));
        cacheConfigs.put(CACHE_ORDER_BY_REFERENCE,
                defaultConfig.entryTtl(orderTtl));
        cacheConfigs.put(CACHE_ORDER_LIST_CUSTOMER,
                defaultConfig.entryTtl(orderListTtl));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}