package io.oigres.ecomm.service.limiter.mps;

import io.opentelemetry.context.Context;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
public class KafkaMessage {

    @Getter
    private final ConsumerRecord<String, Object> record;
    private final Acknowledgment ack;
    private ReentrantLock lock = new ReentrantLock();
    private AtomicBoolean processed = new AtomicBoolean(false);
    private AtomicBoolean acknowledged = new AtomicBoolean(false);
    @Getter
    private Context context = Context.current();

    public void acknowledge() {
        if (!this.processed.get()) {
            return;
        }
        lock.lock();
        try {
            if (!this.acknowledged.get()) {
                ack.acknowledge();
                this.acknowledged.set(true);
            }
        } finally {
            lock.unlock();
        }
    }

    public void setProcessed(boolean value) {
        this.processed.set(value);
    }

    public boolean isProcessed() {
        return this.processed.get();
    }

    public boolean isAcknowledged() {
        return this.acknowledged.get();
    }

}
