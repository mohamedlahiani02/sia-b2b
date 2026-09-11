package tn.sia.b2b.ingestion.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IngestSourceRepository extends JpaRepository<IngestSource, UUID> {

    Optional<IngestSource> findBySourceCodeAndActiveTrue(String sourceCode);

    @Modifying
    @Query("""
        UPDATE IngestSource s
        SET s.lastSeenAt = :at, s.lastSequence = GREATEST(COALESCE(s.lastSequence, -1), :sequence)
        WHERE s.sourceCode = :sourceCode
        """)
    void updateLastSeen(String sourceCode, Long sequence, Instant at);
}
