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

package io.oigres.ecomm.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

public class LockAwareCacheDecorator implements LockableCache {
  private CacheLockFactory cacheLockFactory;
  private Cache delegate;

  protected LockAwareCacheDecorator(CacheLockFactory cacheLockFactory, Cache delegate) {
    this.cacheLockFactory = cacheLockFactory;
    this.delegate = delegate;
  }

  @Override
  public Lock getLock(Object key) {
    String lockName = String.valueOf(key);
    return this.cacheLockFactory.create(lockName);
  }

  private void createLockIfItIsNeeded(Object key) {
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
  @Nullable public ValueWrapper get(Object key) {
    createLockIfItIsNeeded(key);
    return delegate.get(key);
  }

  @Override
  @Nullable public <T> T get(Object key, Class<T> type) {
    createLockIfItIsNeeded(key);
    return delegate.get(key, type);
  }

  @Override
  @Nullable public <T> T get(Object key, Callable<T> valueLoader) {
    createLockIfItIsNeeded(key);
    return delegate.get(key, valueLoader);
  }

  @Override
  @Nullable public CompletableFuture<?> retrieve(Object key) {
    createLockIfItIsNeeded(key);
    return delegate.retrieve(key);
  }

  @Override
  public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
    createLockIfItIsNeeded(key);
    return delegate.retrieve(key, valueLoader);
  }

  @Override
  public void put(Object key, Object value) {
    createLockIfItIsNeeded(key);
    delegate.put(key, value);
  }

  @Override
  @Nullable public ValueWrapper putIfAbsent(Object key, Object value) {
    createLockIfItIsNeeded(key);
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
