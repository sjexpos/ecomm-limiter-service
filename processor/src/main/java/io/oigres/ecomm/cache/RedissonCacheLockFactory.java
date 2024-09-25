package io.oigres.ecomm.cache;

import org.redisson.api.RedissonClient;

import java.util.concurrent.locks.Lock;

public class RedissonCacheLockFactory implements CacheLockFactory {
    private RedissonClient redissonClient;

    public RedissonCacheLockFactory(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Lock create(String name) {
        return redissonClient.getLock(name);
    }
}
