package tn.sia.b2b.ingestion.envelope;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
    @JsonProperty("eventId")       UUID eventId,
    @JsonProperty("eventType")     String eventType,
    @JsonProperty("schemaVersion") int schemaVersion,
    @JsonProperty("source")        String source,
    @JsonProperty("occurredAt")    Instant occurredAt,
    @JsonProperty("sequence")      Long sequence,
    @JsonProperty("entityRef")     String entityRef,
    @JsonProperty("entityVersion") Long entityVersion,
    @JsonProperty("payload")       Object payload
) {}
