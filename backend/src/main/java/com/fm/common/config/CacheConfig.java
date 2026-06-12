package com.fm.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存配置
 *
 * 命名空间（key 格式：fm:{cacheName}:{key}）：
 * - assetSummary     : 资产总览聚合数据，5 分钟过期
 * - assetHoldings    : 持仓明细聚合数据，5 分钟过期
 * - accountList      : 账户列表缓存，5 分钟过期
 *
 * 序列化：value 用 Jackson2JsonRedisSerializer（带 @class 类型信息，支持 LocalDate/LocalDateTime）
 * 连接：spring.data.redis.* 配置（默认 127.0.0.1:6379）
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_ASSET_SUMMARY = "assetSummary";
    public static final String CACHE_ASSET_HOLDINGS = "assetHoldings";
    public static final String CACHE_ACCOUNT_LIST = "accountList";

    public static final String KEY_PREFIX = "fm:";

    private static final Duration TTL = Duration.ofMinutes(5);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // value 序列化器：Jackson + JavaTimeModule + 类型信息（反序列化时能识别具体类）
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // key 序列化器：纯字符串
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TTL)
                .computePrefixWith(cacheName -> KEY_PREFIX + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(java.util.Map.of(
                        CACHE_ASSET_SUMMARY, baseConfig,
                        CACHE_ASSET_HOLDINGS, baseConfig,
                        CACHE_ACCOUNT_LIST, baseConfig
                ))
                .transactionAware()
                .build();
    }
}
