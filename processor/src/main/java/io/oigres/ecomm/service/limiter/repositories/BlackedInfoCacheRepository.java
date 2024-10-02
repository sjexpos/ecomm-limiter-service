package io.oigres.ecomm.service.limiter.repositories;

import io.oigres.ecomm.service.limiter.BlackedInfo;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class BlackedInfoCacheRepository implements BlackedInfoRepository {

    @Cacheable(value = CacheNames.BLACKED_INFO_CACHE_NAME, key = "#userId")
    public BlackedInfo getBlackedInfo(String userId) {
        return null;
    }

    @CachePut(value = CacheNames.BLACKED_INFO_CACHE_NAME, key = "#userId")
    public BlackedInfo storeBlackedInfo(String userId, BlackedInfo blackedInfo) {
        return blackedInfo;
    }

}
