package io.oigres.ecomm.service.limiter.repositories;

import io.oigres.ecomm.service.limiter.model.StorageBucket;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Repository
public class RequestCacheRepository implements RequestRepository {

    @Cacheable(value = CacheNames.REQUEST_CACHE_NAME, key = "#userId+'_'+#time.truncatedTo(T(java.time.temporal.ChronoUnit).MINUTES)")
    public StorageBucket getUserRequestsByTime(String userId, LocalDateTime time) {
        return StorageBucket.builder()
                .userId(userId)
                .minute(time.truncatedTo(ChronoUnit.MINUTES))
                .build();
    }

    @CachePut(value = CacheNames.REQUEST_CACHE_NAME, key = "#userId+'_'+#time.truncatedTo(T(java.time.temporal.ChronoUnit).MINUTES)")
    public StorageBucket storeUserRequests(String userId, LocalDateTime time, StorageBucket data) {
        return data;
    }

}
