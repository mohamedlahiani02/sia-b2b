package tn.sia.b2b.projection.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.sia.b2b.ingestion.envelope.SupportedEventType;
import tn.sia.b2b.ingestion.store.EventInboxEntity;
import tn.sia.b2b.ingestion.store.EventInboxRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * E1-05 — Relais SKIP LOCKED : event_inbox → Kafka.
 *
 * 1. Lèse jusqu'à 100 événements RECEIVED atomiquement (SKIP LOCKED).
 * 2. Publie chaque événement sur le topic Kafka correspondant (acks=all, synchrone).
 * 3. Marque SENT en cas de succès, incrémente tentatives sinon.
 * 4. Après 5 échecs, marque DEAD pour traitement manuel (E10-02).
 */
@Service
public class InboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(InboxRelayService.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 5;
    private static final long KAFKA_TIMEOUT_SECONDS = 5;

    private final EventInboxRepository inboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public InboxRelayService(EventInboxRepository inboxRepository,
                             KafkaTemplate<String, String> kafkaTemplate) {
        this.inboxRepository = inboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 500)
    public void relay() {
        int leased = leaseBatch();
        if (leased == 0) return;

        List<EventInboxEntity> batch = inboxRepository.findLeased();
        for (EventInboxEntity event : batch) {
            processOne(event);
        }
    }

    @Transactional
    public int leaseBatch() {
        return inboxRepository.leaseBatch(BATCH_SIZE);
    }

    @Transactional
    public void processOne(EventInboxEntity event) {
        String topic = SupportedEventType.fromType(event.getEventType())
            .map(SupportedEventType::getTopic)
            .orElse("sia.unknown.events");

        String messageKey = event.getEntityRef();
        String value = buildKafkaMessage(event);

        try {
            kafkaTemplate.send(topic, messageKey, value)
                .get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            event.markSent();
            inboxRepository.save(event);
            log.debug("[RELAY] Envoyé eventId={} topic={}", event.getEventId(), topic);

        } catch (Exception e) {
            int attempts = event.getAttempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                event.markDead(e.getMessage());
                log.error("[RELAY] DEAD après {} tentatives eventId={} : {}",
                    MAX_ATTEMPTS, event.getEventId(), e.getMessage());
            } else {
                event.incrementAttempts(e.getMessage());
                // Repasse à RECEIVED pour prochain cycle
                inboxRepository.resetToReceived(List.of(event.getId()));
                log.warn("[RELAY] Échec tentative {}/{} eventId={} : {}",
                    attempts, MAX_ATTEMPTS, event.getEventId(), e.getMessage());
            }
            inboxRepository.save(event);
        }
    }

    private String buildKafkaMessage(EventInboxEntity event) {
        // Enveloppe compacte pour le consommateur — payload est déjà du JSON
        return "{\"eventId\":\"" + event.getEventId() + "\""
            + ",\"eventType\":\"" + event.getEventType() + "\""
            + ",\"schemaVersion\":" + event.getSchemaVersion()
            + ",\"source\":\"" + event.getSource() + "\""
            + ",\"occurredAt\":\"" + event.getOccurredAt() + "\""
            + ",\"entityRef\":\"" + event.getEntityRef() + "\""
            + ",\"entityVersion\":" + event.getEntityVersion()
            + ",\"payload\":" + event.getPayload()
            + "}";
    }
}
