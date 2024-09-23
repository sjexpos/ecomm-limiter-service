package io.oigres.ecomm.service.limiter.mps.reader;

import io.oigres.ecomm.service.limiter.mps.KafkaMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MessageReader {
    private final PendingQueue<KafkaMessage> pendingQueue;
    private final PendingQueue<KafkaMessage>.OrderIterator orderIterator;
    private final ConcurrentHashMap<String,Object> inflightSet;

    public MessageReader() {
        this.pendingQueue = new PendingQueue<KafkaMessage>();
        this.orderIterator = (PendingQueue<KafkaMessage>.OrderIterator) this.pendingQueue.orderIterator();
        this.inflightSet = new ConcurrentHashMap<>();
    }

    public void queue(ConsumerRecord<String, Object> record, Acknowledgment ack) throws InterruptedException {
        boolean added = false;
        while (!added) {
            added = this.pendingQueue.offer(
                    new KafkaMessage(record, ack),
                    50, TimeUnit.MILLISECONDS
            );
            if (!added && this.pendingQueue.remainingCapacity() == 0) {
                this.pendingQueue.purge(KafkaMessage::isAcknowledged, Duration.ofMillis(50));
            }
        }
    }

    public KafkaMessage poll() throws InterruptedException {
        KafkaMessage msg = null;
        while (msg == null) {
            msg = this.orderIterator.next(Duration.ofMillis(100));
        }
        if (msg != null) {
            this.inflightSet.put(msg.getRecord().key(), msg);
        }
        return msg;
    }

    public void acknowledgeIfPossible(KafkaMessage message) {
        final AtomicBoolean wasPrevAcknowledged = new AtomicBoolean(true);
        this.pendingQueue.forEach(m -> {
                    if (m.getRecord().partition() != message.getRecord().partition()) {
                        return;
                    }
                    if (wasPrevAcknowledged.get()) {
                        m.acknowledge();
                    }
                    wasPrevAcknowledged.set(m.isAcknowledged());
                },
                m -> (m.getRecord().partition() == message.getRecord().partition()) && !m.isAcknowledged());
    }

}
