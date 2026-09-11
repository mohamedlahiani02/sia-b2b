package tn.sia.b2b.ingestion.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EventInboxRepository extends JpaRepository<EventInboxEntity, UUID> {

    boolean existsByEventId(UUID eventId);

    /**
     * SKIP LOCKED : sélectionne et lèse atomiquement les événements RECEIVED.
     * Évite que deux instances relay traitent le même événement simultanément.
     */
    @Modifying
    @Query(value = """
        UPDATE event_inbox SET state = 'LEASED'
        WHERE id IN (
            SELECT id FROM event_inbox
            WHERE state = 'RECEIVED'
            ORDER BY received_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
        )
        """, nativeQuery = true)
    int leaseBatch(int limit);

    @Query("SELECT e FROM EventInboxEntity e WHERE e.state = 'LEASED' ORDER BY e.occurredAt ASC")
    List<EventInboxEntity> findLeased();

    @Query("SELECT e FROM EventInboxEntity e WHERE e.state = 'DEAD' ORDER BY e.receivedAt DESC")
    Page<EventInboxEntity> findDead(Pageable pageable);

    @Query("SELECT e FROM EventInboxEntity e WHERE e.state = 'RECEIVED' ORDER BY e.receivedAt DESC")
    Page<EventInboxEntity> findPending(Pageable pageable);

    @Modifying
    @Query("UPDATE EventInboxEntity e SET e.state = 'RECEIVED', e.lastError = null WHERE e.id IN :ids")
    int resetToReceived(List<UUID> ids);

    @Query("SELECT COUNT(e) FROM EventInboxEntity e WHERE e.source = :source AND e.state = 'DEAD'")
    long countDead(String source);
}
