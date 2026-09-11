package tn.sia.b2b.ops.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.sia.b2b.ingestion.auth.IngestSourceRepository;
import tn.sia.b2b.ingestion.store.EventInboxEntity;
import tn.sia.b2b.ingestion.store.EventInboxRepository;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SyncHealthService {

    private final JdbcTemplate jdbc;
    private final EventInboxRepository inboxRepository;
    private final IngestSourceRepository sourceRepository;

    public SyncHealthService(JdbcTemplate jdbc,
                             EventInboxRepository inboxRepository,
                             IngestSourceRepository sourceRepository) {
        this.jdbc = jdbc;
        this.inboxRepository = inboxRepository;
        this.sourceRepository = sourceRepository;
    }

    /**
     * E10-01 — Vue d'ensemble de la santé du flux de synchronisation.
     */
    public SyncHealthReport getReport() {
        List<SourceHealth> sources = sourceRepository.findAll().stream()
            .map(s -> new SourceHealth(
                s.getSourceCode(),
                s.isActive(),
                s.getLastSequence(),
                inboxRepository.countDead(s.getSourceCode()),
                querySyncHealth(s.getSourceCode())
            ))
            .toList();

        long totalPending = jdbc.queryForObject(
            "SELECT COUNT(*) FROM event_inbox WHERE state = 'RECEIVED'", Long.class);
        long totalDead = jdbc.queryForObject(
            "SELECT COUNT(*) FROM event_inbox WHERE state = 'DEAD'", Long.class);

        return new SyncHealthReport(Instant.now(), sources, totalPending, totalDead);
    }

    /**
     * E10-02 — Liste paginée des événements DEAD.
     */
    public Page<EventInboxEntity> findDeadEvents(int page, int size) {
        return inboxRepository.findDead(PageRequest.of(page, Math.min(size, 200)));
    }

    /**
     * E10-03 — Rejeu manuel : remet les événements DEAD à l'état RECEIVED.
     */
    @Transactional
    public int replayEvents(List<UUID> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "eventIds est obligatoire");
        }
        if (eventIds.size() > 100) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Maximum 100 événements par rejeu");
        }
        return inboxRepository.resetToReceived(eventIds);
    }

    private List<Map<String, Object>> querySyncHealth(String sourceCode) {
        return jdbc.queryForList("""
            SELECT window_start, events_received, events_applied, events_dead, max_lag_ms
            FROM sync_health
            WHERE source_code = ?
            ORDER BY window_start DESC
            LIMIT 24
            """, sourceCode);
    }

    public record SyncHealthReport(
        Instant reportedAt,
        List<SourceHealth> sources,
        long totalPendingInbox,
        long totalDeadInbox
    ) {}

    public record SourceHealth(
        String sourceCode,
        boolean active,
        Long lastSequence,
        long deadEvents,
        List<Map<String, Object>> recentWindows
    ) {}
}
