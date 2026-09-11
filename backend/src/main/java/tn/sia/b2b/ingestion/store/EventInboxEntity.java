package tn.sia.b2b.ingestion.store;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_inbox")
public class EventInboxEntity {

    @Id
    private UUID id;

    @Column(name = "event_id", unique = true, nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "sequence")
    private Long sequence;

    @Column(name = "entity_ref", nullable = false)
    private String entityRef;

    @Column(name = "entity_version", nullable = false)
    private Long entityVersion;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "state", nullable = false)
    private String state = InboxState.RECEIVED.name();

    @Column(name = "attempts")
    private int attempts = 0;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected EventInboxEntity() {}

    public static EventInboxEntity create(UUID id, UUID eventId, String eventType, int schemaVersion,
                                          String source, Long sequence, String entityRef,
                                          Long entityVersion, Instant occurredAt, String payloadJson) {
        EventInboxEntity e = new EventInboxEntity();
        e.id = id;
        e.eventId = eventId;
        e.eventType = eventType;
        e.schemaVersion = schemaVersion;
        e.source = source;
        e.sequence = sequence;
        e.entityRef = entityRef;
        e.entityVersion = entityVersion;
        e.occurredAt = occurredAt;
        e.receivedAt = Instant.now();
        e.payload = payloadJson;
        e.state = InboxState.RECEIVED.name();
        return e;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public int getSchemaVersion() { return schemaVersion; }
    public String getSource() { return source; }
    public Long getSequence() { return sequence; }
    public String getEntityRef() { return entityRef; }
    public Long getEntityVersion() { return entityVersion; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getPayload() { return payload; }
    public String getState() { return state; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }

    public void markSent() {
        this.state = InboxState.SENT.name();
        this.processedAt = Instant.now();
    }

    public void markDead(String error) {
        this.state = InboxState.DEAD.name();
        this.lastError = error;
        this.attempts++;
    }

    public void incrementAttempts(String error) {
        this.attempts++;
        this.lastError = error;
    }
}
