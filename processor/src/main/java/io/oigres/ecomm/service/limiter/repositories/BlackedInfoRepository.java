package io.oigres.ecomm.service.limiter.repositories;

import io.oigres.ecomm.service.limiter.BlackedInfo;

public interface BlackedInfoRepository {

    BlackedInfo getBlackedInfo(String userId);

    BlackedInfo storeBlackedInfo(String userId, BlackedInfo blackedInfo);

}
