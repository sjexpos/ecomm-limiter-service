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

import java.util.Map;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

public class RedisLockAwareCacheManager extends RedisCacheManager {
  private CacheLockFactory cacheLockFactory;

  public RedisLockAwareCacheManager(
      RedisCacheWriter cacheWriter,
      RedisCacheConfiguration defaultCacheConfiguration,
      boolean allowRuntimeCacheCreation,
      Map<String, RedisCacheConfiguration> initialCacheConfigurations,
      CacheLockFactory cacheLockFactory) {
    super(
        cacheWriter,
        defaultCacheConfiguration,
        allowRuntimeCacheCreation,
        initialCacheConfigurations);
    this.cacheLockFactory = cacheLockFactory;
  }

  @Override
  protected Cache decorateCache(Cache cache) {
    return new LockAwareCacheDecorator(this.cacheLockFactory, cache);
  }
}
