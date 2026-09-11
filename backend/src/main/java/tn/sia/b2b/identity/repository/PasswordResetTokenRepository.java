package tn.sia.b2b.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.sia.b2b.identity.domain.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
