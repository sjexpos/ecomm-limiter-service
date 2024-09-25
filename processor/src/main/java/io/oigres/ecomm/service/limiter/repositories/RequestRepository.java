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

public interface RequestRepository {

    StorageBucket getUserRequestsByTime(String userId, LocalDateTime time);

    StorageBucket storeUserRequests(String userId, LocalDateTime time, StorageBucket data);

}
