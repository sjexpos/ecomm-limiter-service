package io.oigres.ecomm.service.limiter.repositories;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

import io.oigres.ecomm.service.limiter.model.StorageBucket;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import io.oigres.ecomm.service.limiter.model.RequestData;

@Repository
public class RequestRepository {

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
