package tn.sia.b2b.ingestion.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingest_source")
public class IngestSource {

    @Id
    private UUID id;

    @Column(name = "source_code", unique = true, nullable = false)
    private String sourceCode;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(name = "allowed_ips", columnDefinition = "text[]")
    private String[] allowedIps;

    @Column(name = "last_sequence")
    private Long lastSequence;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "active")
    private boolean active = true;

    protected IngestSource() {}

    public UUID getId() { return id; }
    public String getSourceCode() { return sourceCode; }
    public String getSecretHash() { return secretHash; }
    public String[] getAllowedIps() { return allowedIps; }
    public Long getLastSequence() { return lastSequence; }
    public boolean isActive() { return active; }

    public void updateLastSeen(Instant at) {
        this.lastSeenAt = at;
    }

    public void updateLastSequence(Long sequence) {
        this.lastSequence = sequence;
    }
}
