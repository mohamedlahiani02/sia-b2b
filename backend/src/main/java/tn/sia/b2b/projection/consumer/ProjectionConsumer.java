package tn.sia.b2b.projection.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * E1-06 — Consommateurs @KafkaListener avec offset manuel.
 *
 * Un listener par topic pour isoler les erreurs par flux.
 * Le contrôle d'ordre par entityVersion est délégué à ProjectionUpdater.
 * En cas d'erreur persistante (3 tentatives), le DefaultErrorHandler
 * route vers le DLT correspondant (ex: sia.ingest.product.v1.dlt).
 */
@Component
public class ProjectionConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProjectionConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProjectionUpdater updater;

    public ProjectionConsumer(ObjectMapper objectMapper, ProjectionUpdater updater) {
        this.objectMapper = objectMapper;
        this.updater = updater;
    }

    @KafkaListener(topics = "sia.ingest.product.v1", groupId = "sia-b2b-projections",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeProduct(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ack);
    }

    @KafkaListener(topics = "sia.ingest.stock.v1", groupId = "sia-b2b-projections",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeStock(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ack);
    }

    @KafkaListener(topics = "sia.ingest.price.v1", groupId = "sia-b2b-projections",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumePrice(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ack);
    }

    @KafkaListener(topics = "sia.ingest.customer.v1", groupId = "sia-b2b-projections",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeCustomer(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ack);
    }

    @KafkaListener(topics = "sia.ingest.order-feedback.v1", groupId = "sia-b2b-projections",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeOrderFeedback(ConsumerRecord<String, String> record, Acknowledgment ack) {
        process(record, ack);
    }

    private void process(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ProjectionEvent event = objectMapper.readValue(record.value(), ProjectionEvent.class);
            log.debug("[CONSUMER] topic={} partition={} offset={} eventType={} entityRef={}",
                record.topic(), record.partition(), record.offset(),
                event.eventType(), event.entityRef());

            updater.apply(event);
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[CONSUMER] Erreur traitement topic={} offset={} : {}",
                record.topic(), record.offset(), e.getMessage());
            // Ne pas acquitter → DefaultErrorHandler retente (x2) puis envoie au DLT
            throw new RuntimeException("Erreur projection", e);
        }
    }
}
