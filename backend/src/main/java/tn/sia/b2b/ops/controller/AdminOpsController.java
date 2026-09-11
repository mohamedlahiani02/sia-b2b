package tn.sia.b2b.ops.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.sia.b2b.ingestion.store.EventInboxEntity;
import tn.sia.b2b.ops.service.SyncHealthService;

import java.util.List;
import java.util.UUID;

/**
 * E10-01 à E10-03 — Endpoints de supervision du flux événementiel.
 * Accès restreint : ADMIN ou OPERATOR.
 */
@RestController
@RequestMapping("/api/v1/admin/sync")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AdminOpsController {

    private final SyncHealthService syncHealthService;

    public AdminOpsController(SyncHealthService syncHealthService) {
        this.syncHealthService = syncHealthService;
    }

    /**
     * E10-01 — Santé globale du flux de synchronisation.
     */
    @GetMapping("/health")
    public ResponseEntity<SyncHealthService.SyncHealthReport> health() {
        return ResponseEntity.ok(syncHealthService.getReport());
    }

    /**
     * E10-02 — File d'erreurs : événements DEAD, paginés.
     */
    @GetMapping("/dead-events")
    public ResponseEntity<Page<DeadEventView>> deadEvents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        Page<DeadEventView> result = syncHealthService.findDeadEvents(page, size)
            .map(DeadEventView::from);
        return ResponseEntity.ok(result);
    }

    /**
     * E10-03 — Rejeu manuel d'événements DEAD (max 100 par appel).
     * Réservé aux ADMIN.
     */
    @PostMapping("/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReplayResult> replay(@RequestBody ReplayRequest request) {
        int reset = syncHealthService.replayEvents(request.eventIds());
        return ResponseEntity.ok(new ReplayResult(reset, request.eventIds().size()));
    }

    public record DeadEventView(
        UUID id,
        UUID eventId,
        String eventType,
        String source,
        String entityRef,
        Long entityVersion,
        int attempts,
        String lastError
    ) {
        static DeadEventView from(EventInboxEntity e) {
            return new DeadEventView(
                e.getId(), e.getEventId(), e.getEventType(),
                e.getSource(), e.getEntityRef(), e.getEntityVersion(),
                e.getAttempts(), e.getLastError()
            );
        }
    }

    public record ReplayRequest(List<UUID> eventIds) {}

    public record ReplayResult(int reset, int requested) {}
}
