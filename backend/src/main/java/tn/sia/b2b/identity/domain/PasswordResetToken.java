package tn.sia.b2b.identity.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used;

    protected PasswordResetToken() {}

    public static PasswordResetToken create(UUID id, UUID userId, String tokenHash, Instant expiresAt) {
        PasswordResetToken t = new PasswordResetToken();
        t.id = id;
        t.userId = userId;
        t.tokenHash = tokenHash;
        t.expiresAt = expiresAt;
        t.used = false;
        return t;
    }

    public boolean isValid() {
        return !used && Instant.now().isBefore(expiresAt);
    }

    public void markUsed() {
        this.used = true;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
}
