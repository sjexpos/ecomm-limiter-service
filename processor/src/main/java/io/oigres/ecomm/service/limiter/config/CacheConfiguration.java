/**********
 This project is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the
 Free Software Foundation; either version 3.0 of the License, or (at your
 option) any later version. (See <https://www.gnu.org/licenses/gpl-3.0.html>.)

 This project is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 more details.

 You should have received a copy of the GNU General Public License
 along with this project; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 **********/
// Copyright (c) 2024-2025 Sergio Exposito.  All rights reserved.              

package io.oigres.ecomm.service.limiter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.oigres.ecomm.cache.CacheLockFactory;
import io.oigres.ecomm.cache.GzipRedisSerializer;
import io.oigres.ecomm.cache.RedisLockAwareCacheManager;
import io.oigres.ecomm.cache.RedissonCacheLockFactory;
import io.oigres.ecomm.service.limiter.BlackedInfo;
import io.oigres.ecomm.service.limiter.model.StorageBucket;
import io.oigres.ecomm.service.limiter.repositories.CacheNames;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.redisson.api.RedissonClient;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@EnableCaching(mode = AdviceMode.PROXY)
public class CacheConfiguration {

  @Bean
  public RedissonConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
    return new RedissonConnectionFactory(redissonClient);
  }

  @Bean
  public CacheLockFactory cacheLockFactory(RedissonClient redissonClient) {
    return new RedissonCacheLockFactory(redissonClient);
  }

  @Bean
  public CacheManager cacheManager(
      RedissonConnectionFactory connectionFactory, CacheLockFactory cacheLockFactory) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    cacheConfigurations.put(
        CacheNames.REQUEST_CACHE_NAME,
        RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GzipRedisSerializer<>(
                        new Jackson2JsonRedisSerializer<>(objectMapper, StorageBucket.class))))
            .entryTtl(Duration.ofMinutes(2)));
    cacheConfigurations.put(
        CacheNames.BLACKED_INFO_CACHE_NAME,
        RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GzipRedisSerializer<>(
                        new Jackson2JsonRedisSerializer<>(objectMapper, BlackedInfo.class))))
            .entryTtl(Duration.ofHours(12)));

    RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);
    cacheWriter.withStatisticsCollector(CacheStatisticsCollector.create());
    RedisCacheManager cacheManager =
        new RedisLockAwareCacheManager(
            cacheWriter,
            RedisCacheConfiguration.defaultCacheConfig(),
            true,
            cacheConfigurations,
            cacheLockFactory);
    cacheManager.setTransactionAware(false);
    return cacheManager;
  }
}
