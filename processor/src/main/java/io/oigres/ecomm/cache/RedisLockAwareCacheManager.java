package io.oigres.ecomm.cache;

import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.Map;

public class RedisLockAwareCacheManager extends RedisCacheManager {
    private CacheLockFactory cacheLockFactory;

    public RedisLockAwareCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration, boolean allowRuntimeCacheCreation, Map<String, RedisCacheConfiguration> initialCacheConfigurations, CacheLockFactory cacheLockFactory) {
        super(cacheWriter, defaultCacheConfiguration, allowRuntimeCacheCreation, initialCacheConfigurations);
        this.cacheLockFactory = cacheLockFactory;
    }

    @Override
    protected Cache decorateCache(Cache cache) {
        return new LockAwareCacheDecorator(this.cacheLockFactory, cache);
    }

}
