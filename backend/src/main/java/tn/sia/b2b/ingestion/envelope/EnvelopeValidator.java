package tn.sia.b2b.ingestion.envelope;

import org.springframework.stereotype.Component;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;

import java.util.ArrayList;
import java.util.List;

@Component
public class EnvelopeValidator {

    /**
     * Valide la structure de l'enveloppe sans valider le payload métier (TBD-01 à TBD-04).
     * Retourne la liste des violations, vide si valide.
     */
    public List<String> validate(EventEnvelope envelope) {
        List<String> errors = new ArrayList<>();

        if (envelope.eventId() == null) {
            errors.add("eventId est obligatoire");
        }
        if (envelope.eventType() == null || envelope.eventType().isBlank()) {
            errors.add("eventType est obligatoire");
        }
        if (envelope.source() == null || envelope.source().isBlank()) {
            errors.add("source est obligatoire");
        }
        if (envelope.occurredAt() == null) {
            errors.add("occurredAt est obligatoire");
        }
        if (envelope.entityRef() == null || envelope.entityRef().isBlank()) {
            errors.add("entityRef est obligatoire");
        }
        if (envelope.entityVersion() == null || envelope.entityVersion() < 0) {
            errors.add("entityVersion est obligatoire et doit être >= 0");
        }
        if (envelope.payload() == null) {
            errors.add("payload est obligatoire");
        }

        if (!errors.isEmpty()) {
            return errors;
        }

        if (!SupportedEventType.SUPPORTED_SCHEMA_VERSIONS.contains(envelope.schemaVersion())) {
            errors.add("schemaVersion " + envelope.schemaVersion() + " non supporté");
            return errors;
        }

        if (SupportedEventType.fromType(envelope.eventType()).isEmpty()) {
            errors.add("eventType inconnu : " + envelope.eventType());
        }

        return errors;
    }

    public ValidationResult validateFull(EventEnvelope envelope) {
        List<String> errors = validate(envelope);
        if (errors.isEmpty()) {
            return ValidationResult.ok();
        }
        ErrorCode code = errors.stream().anyMatch(e -> e.contains("schemaVersion"))
            ? ErrorCode.INGEST_SCHEMA_UNSUPPORTED
            : ErrorCode.INGEST_ENVELOPE_INVALID;
        return ValidationResult.error(code, errors);
    }

    public record ValidationResult(boolean valid, ErrorCode errorCode, List<String> errors) {
        static ValidationResult ok() {
            return new ValidationResult(true, null, List.of());
        }
        static ValidationResult error(ErrorCode code, List<String> errors) {
            return new ValidationResult(false, code, errors);
        }
    }
}
