package io.oigres.ecomm.cache;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public class LockAwareCacheDecorator implements Cache {
    private CacheLockFactory cacheLockFactory;
    private Cache delegate;

    protected LockAwareCacheDecorator(CacheLockFactory cacheLockFactory, Cache delegate) {
        this.cacheLockFactory = cacheLockFactory;
        this.delegate = delegate;
    }

    private void createLock(Object key) {
        String lockName = String.valueOf(key);
        if (CacheLockManager.isSynchronizationActive()) {
            Lock lock = CacheLockManager.getLocks(lockName);
            if (lock == null) {
                lock = this.cacheLockFactory.create(lockName);
                CacheLockManager.registerLock(lock, lockName);
                lock.lock();
            }
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        createLock(key);
        return delegate.get(key);
    }

    @Override
    @Nullable
    public <T> T get(Object key, Class<T> type) {
        createLock(key);
        return delegate.get(key, type);
    }

    @Override
    @Nullable
    public <T> T get(Object key, Callable<T> valueLoader) {
        createLock(key);
        return delegate.get(key, valueLoader);
    }

    @Override
    @Nullable
    public CompletableFuture<?> retrieve(Object key) {
        createLock(key);
        return delegate.retrieve(key);
    }

    @Override
    public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
        createLock(key);
        return delegate.retrieve(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        createLock(key);
        delegate.put(key, value);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, Object value) {
        createLock(key);
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        return delegate.evictIfPresent(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean invalidate() {
        return delegate.invalidate();
    }
}
