package tn.sia.b2b.projection.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record ProjectionEvent(
    @JsonProperty("eventId")       UUID eventId,
    @JsonProperty("eventType")     String eventType,
    @JsonProperty("schemaVersion") int schemaVersion,
    @JsonProperty("source")        String source,
    @JsonProperty("occurredAt")    Instant occurredAt,
    @JsonProperty("entityRef")     String entityRef,
    @JsonProperty("entityVersion") long entityVersion,
    @JsonProperty("payload")       Object payload
) {}
