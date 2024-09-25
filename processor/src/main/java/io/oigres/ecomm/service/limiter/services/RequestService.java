package io.oigres.ecomm.service.limiter.services;

import io.oigres.ecomm.service.limiter.RequestAudit;
import io.oigres.ecomm.service.limiter.ResponseAudit;

public interface RequestService {

    void requestArrive(RequestAudit request);

    void responseArrive(ResponseAudit response);

}
