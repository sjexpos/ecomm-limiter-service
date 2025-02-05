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

package io.oigres.ecomm.service.limiter.repositories;

import io.oigres.ecomm.service.limiter.model.StorageBucket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class RequestCacheRepository implements RequestRepository {

  @Cacheable(
      value = CacheNames.REQUEST_CACHE_NAME,
      key = "#userId+'_'+#time.truncatedTo(T(java.time.temporal.ChronoUnit).MINUTES)")
  public StorageBucket getUserRequestsByTime(String userId, LocalDateTime time) {
    return StorageBucket.builder()
        .userId(userId)
        .minute(time.truncatedTo(ChronoUnit.MINUTES))
        .build();
  }

  @CachePut(
      value = CacheNames.REQUEST_CACHE_NAME,
      key = "#userId+'_'+#time.truncatedTo(T(java.time.temporal.ChronoUnit).MINUTES)")
  public StorageBucket storeUserRequests(String userId, LocalDateTime time, StorageBucket data) {
    return data;
  }
}
