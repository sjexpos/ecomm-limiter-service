package io.oigres.ecomm.service.limiter.services;

import io.oigres.ecomm.cache.annotations.CacheLock;
import io.oigres.ecomm.service.limiter.BlackedInfo;
import io.oigres.ecomm.service.limiter.RequestAudit;
import io.oigres.ecomm.service.limiter.ResponseAudit;
import io.oigres.ecomm.service.limiter.model.RequestData;
import io.oigres.ecomm.service.limiter.model.StorageBucket;
import io.oigres.ecomm.service.limiter.repositories.BlackedInfoRepository;
import io.oigres.ecomm.service.limiter.repositories.RequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RequestServiceImpl implements RequestService {

    private final int rateLimit; // TODO - this rate limit should be different per user according to some rule. It's just a demo!
    private final RequestRepository requestRepository;
    private final BlackedInfoRepository blackedInfoRepository;
    private final KafkaTemplate<String, BlackedInfo> kafkaTemplate;

    public RequestServiceImpl(
            RequestRepository requestRepository,
            BlackedInfoRepository blackedInfoRepository,
            KafkaTemplate<String, BlackedInfo> kafkaTemplate,
            @Value("${ecomm.service.limiter.rate-limit}") int rateLimit
    ) {
        this.requestRepository = requestRepository;
        this.blackedInfoRepository = blackedInfoRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.rateLimit = rateLimit;
    }

    private List<RequestData> addSorted(List<RequestData> requests, RequestData data) {
        if (requests.isEmpty()) {
            requests.add(data);
            return requests;
        }
        int index = requests.size()-1;
        for (; index >=0; index--) {
            if (requests.get(index).getRequestArrived().isBefore(data.getRequestArrived())) {
                break;
            }
        }
        requests.add(index, data);
        return requests;
    }

    private BlackedInfo broadcastBlacklistedUser(String userId) {
        LocalDateTime blockedFrom = LocalDateTime.now();
        LocalDateTime blockedTo = blockedFrom.plusMinutes(1);
        BlackedInfo blackedInfo = BlackedInfo.builder()
                .userId(userId)
                .from(blockedFrom)
                .to(blockedTo)
                .build();
        log.info("User '{}' will be blocked from {} to {}", blackedInfo.getUserId(), blackedInfo.getFrom(), blackedInfo.getTo());
        this.kafkaTemplate.sendDefault(userId, blackedInfo);
        return blackedInfo;
    }

    private void blacklistUser(String userId) {
        BlackedInfo blackedInfo = this.blackedInfoRepository.getBlackedInfo(userId);
        if (blackedInfo != null && blackedInfo.isIncluded(LocalDateTime.now())) {
            return;
        }
        blackedInfo = broadcastBlacklistedUser(userId);
        this.blackedInfoRepository.storeBlackedInfo(userId, blackedInfo);
    }

    @CacheLock
    public void requestArrive(RequestAudit request) {
        log.info("Processing request for user {}", request.getUserId());
        if (!StringUtils.hasText(request.getUserId())) {
            return;
        }
        StorageBucket bucket = this.requestRepository.getUserRequestsByTime(request.getUserId(), request.getArrived());
        addSorted(bucket.getRequests(), RequestData.builder()
                .request(request)
                .requestArrived(LocalDateTime.now())
                .build()
        );
        this.requestRepository.storeUserRequests(request.getUserId(), request.getArrived(), bucket);
        if (bucket.getRequests().size() > this.rateLimit) {
            blacklistUser(request.getUserId());
        }
    }

    @CacheLock
    public void responseArrive(ResponseAudit response) {
        log.info("Processing response for user {}", response.getUserId());
        if (!StringUtils.hasText(response.getUserId())) {
            return;
        }
        Optional<RequestData> data = Optional.empty();
        StorageBucket bucket = null;
        LocalDateTime time = response.getArrived();
        while (!data.isPresent()) {
            bucket = this.requestRepository.getUserRequestsByTime(response.getUserId(), time);
            if (bucket == null || bucket.getRequests().isEmpty()) {
                break;
            }
            data = bucket.getRequests().reversed().stream().filter(
                            request -> request.getRequest().getId().equals(response.getId())
                    )
                    .findAny();
            time = time.minusMinutes(1);
        }
        if (data.isPresent()) {
            data.get().setResponse(response);
            data.get().setResponseArrived(time);
            this.requestRepository.storeUserRequests(response.getUserId(), time, bucket);
        }
    }

}
