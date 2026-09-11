package tn.sia.b2b.ingestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.sia.b2b.ingestion.auth.IngestSource;
import tn.sia.b2b.ingestion.auth.IngestSourceRepository;
import tn.sia.b2b.ingestion.envelope.EnvelopeValidator;
import tn.sia.b2b.ingestion.envelope.EventEnvelope;
import tn.sia.b2b.ingestion.store.EventInboxEntity;
import tn.sia.b2b.ingestion.store.EventInboxRepository;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;

import java.time.Instant;
import java.util.UUID;

@Service
public class EventIngestionService {

    private final EventInboxRepository inboxRepository;
    private final IngestSourceRepository sourceRepository;
    private final EnvelopeValidator validator;
    private final SequenceGapDetector gapDetector;
    private final ObjectMapper objectMapper;

    public EventIngestionService(EventInboxRepository inboxRepository,
                                 IngestSourceRepository sourceRepository,
                                 EnvelopeValidator validator,
                                 SequenceGapDetector gapDetector,
                                 ObjectMapper objectMapper) {
        this.inboxRepository = inboxRepository;
        this.sourceRepository = sourceRepository;
        this.validator = validator;
        this.gapDetector = gapDetector;
        this.objectMapper = objectMapper;
    }

    public enum IngestStatus { ACCEPTED, DUPLICATE, REJECTED, SCHEMA_UNSUPPORTED }

    public record ElementResult(UUID eventId, IngestStatus status, String errorCode, String error) {
        public static ElementResult accepted(UUID id) {
            return new ElementResult(id, IngestStatus.ACCEPTED, null, null);
        }
        public static ElementResult duplicate(UUID id) {
            return new ElementResult(id, IngestStatus.DUPLICATE, null, null);
        }
        public static ElementResult rejected(UUID id, ErrorCode code, String message) {
            return new ElementResult(id, IngestStatus.REJECTED, code.name(), message);
        }
    }

    /**
     * Ingère un seul événement dans l'inbox. Idempotent via contrainte UNIQUE sur event_id.
     * Chaque événement est traité dans sa propre transaction pour éviter qu'un échec
     * n'annule les événements précédents du même batch.
     */
    @Transactional
    public ElementResult ingest(EventEnvelope envelope, IngestSource source) {
        // Idempotence : déjà reçu → 202 silencieux
        if (inboxRepository.existsByEventId(envelope.eventId())) {
            return ElementResult.duplicate(envelope.eventId());
        }

        // Validation enveloppe sans validation payload (TBD-01 à TBD-04)
        EnvelopeValidator.ValidationResult validation = validator.validateFull(envelope);
        if (!validation.valid()) {
            return ElementResult.rejected(envelope.eventId(), validation.errorCode(),
                String.join("; ", validation.errors()));
        }

        // Détection trou de séquence (non bloquant)
        if (envelope.sequence() != null) {
            gapDetector.checkAndLog(source.getSourceCode(), source.getLastSequence(), envelope.sequence());
        }

        // Sérialisation payload
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(envelope.payload());
        } catch (JsonProcessingException e) {
            return ElementResult.rejected(envelope.eventId(), ErrorCode.INGEST_ENVELOPE_INVALID,
                "Payload non sérialisable");
        }

        EventInboxEntity entity = EventInboxEntity.create(
            UuidCreator.getTimeOrderedEpoch(),
            envelope.eventId(),
            envelope.eventType(),
            envelope.schemaVersion(),
            envelope.source(),
            envelope.sequence(),
            envelope.entityRef(),
            envelope.entityVersion(),
            envelope.occurredAt(),
            payloadJson
        );

        inboxRepository.save(entity);

        // Mise à jour last_sequence (best effort — erreur non bloquante)
        if (envelope.sequence() != null) {
            try {
                sourceRepository.updateLastSeen(source.getSourceCode(), envelope.sequence(), Instant.now());
            } catch (Exception ignored) {}
        }

        return ElementResult.accepted(envelope.eventId());
    }
}
