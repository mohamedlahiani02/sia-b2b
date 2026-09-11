package tn.sia.b2b.projection.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.sia.b2b.ingestion.service.SnapshotChunkAccumulator;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Applique les événements Kafka sur les tables de projection.
 *
 * Contrôle d'ordre par entityVersion (E1-06) :
 *   - Si source_version >= entityVersion → événement obsolète, ignoré.
 *   - Sinon → UPSERT sur la table cible.
 *
 * Les colonnes métier (TBD-01 à TBD-04) ne sont pas encore définies.
 * On stocke le payload brut dans payload_raw jusqu'à la session YME.
 */
@Component
public class ProjectionUpdater {

    private static final Logger log = LoggerFactory.getLogger(ProjectionUpdater.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final SnapshotChunkAccumulator snapshotAccumulator;

    public ProjectionUpdater(JdbcTemplate jdbc, ObjectMapper objectMapper,
                             SnapshotChunkAccumulator snapshotAccumulator) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.snapshotAccumulator = snapshotAccumulator;
    }

    @Transactional
    public void apply(ProjectionEvent event) {
        String type = event.eventType();

        if (type.startsWith("product.")) {
            applyProductEvent(event);
        } else if (type.equals("stock.snapshot")) {
            applyStockSnapshot(event);
        } else if (type.startsWith("stock.")) {
            applyStockEvent(event);
        } else if (type.startsWith("price.")) {
            applyPriceEvent(event);
        } else if (type.startsWith("customer.")) {
            applyCustomerEvent(event);
        } else if (type.startsWith("order.")) {
            applyOrderEvent(event);
        } else {
            log.warn("[PROJECTION] Type inconnu : {}", type);
        }
    }

    private void applyProductEvent(ProjectionEvent event) {
        String status = event.eventType().equals("product.deactivated") ? "INACTIVE" : "ACTIVE";
        upsertProjection("product_projection", event, status);
    }

    private void applyStockEvent(ProjectionEvent event) {
        upsertProjection("stock_projection", event, "ACTIVE");
    }

    private void applyPriceEvent(ProjectionEvent event) {
        upsertProjection("price_projection", event, "ACTIVE");
    }

    private void applyCustomerEvent(ProjectionEvent event) {
        upsertProjection("customer_projection", event, "ACTIVE");
    }

    // order.confirmed / order.partially_confirmed / order.rejected
    // Pas de table order_projection : ces événements déclenchent une mise à jour
    // de l'état de la commande dans la table orders (Lot 5 — post-YME).
    // Pour l'instant : log uniquement, pas de projection dédiée.
    private void applyOrderEvent(ProjectionEvent event) {
        log.info("[PROJECTION] Événement commande reçu eventType={} entityRef={} — traitement Lot 5",
            event.eventType(), event.entityRef());
    }

    /**
     * E1-08 — Réception snapshot par tranches.
     * Le payload contient : snapshotId, chunkIndex, totalChunks, items[].
     */
    private void applyStockSnapshot(ProjectionEvent event) {
        try {
            Map<?, ?> payload = objectMapper.convertValue(event.payload(), Map.class);
            String snapshotId = String.valueOf(payload.get("snapshotId"));
            int chunkIndex = ((Number) payload.get("chunkIndex")).intValue();
            int totalChunks = ((Number) payload.get("totalChunks")).intValue();
            String payloadJson = objectMapper.writeValueAsString(payload);

            boolean complete = snapshotAccumulator.recordChunk(snapshotId, chunkIndex, totalChunks, payloadJson);
            if (complete) {
                applyCompleteSnapshot(snapshotId, totalChunks, event);
                snapshotAccumulator.cleanup(snapshotId, totalChunks);
            }
        } catch (Exception e) {
            log.error("[SNAPSHOT] Erreur traitement chunk pour eventId={} : {}", event.eventId(), e.getMessage());
            throw new RuntimeException("Erreur snapshot chunk", e);
        }
    }

    private void applyCompleteSnapshot(String snapshotId, int totalChunks, ProjectionEvent event) throws Exception {
        log.info("[SNAPSHOT] Application snapshot complet snapshotId={}", snapshotId);
        for (int i = 0; i < totalChunks; i++) {
            String chunkJson = snapshotAccumulator.getChunk(snapshotId, i)
                .orElseThrow(() -> new IllegalStateException("Tranche " + i + " manquante pour snapshot " + snapshotId));
            Map<?, ?> chunk = objectMapper.readValue(chunkJson, Map.class);
            // Les items sont traités individuellement — structure payload_raw
            // Les colonnes métier seront ajoutées après session YME (TBD-02)
            log.debug("[SNAPSHOT] Tranche {}/{} appliquée pour snapshotId={}", i + 1, totalChunks, snapshotId);
        }
        // Marquer snapshot complet dans stock_projection (via source_ref spéciale)
        jdbc.update("""
            INSERT INTO stock_projection (id, source_ref, source_version, last_event_id,
                projected_at, source_occurred_at, payload_raw, status)
            VALUES (gen_random_uuid(), ?, 0, ?, now(), ?, '{}'::jsonb, 'SNAPSHOT_COMPLETE')
            ON CONFLICT (source_ref) DO UPDATE SET
                source_version = EXCLUDED.source_version,
                last_event_id = EXCLUDED.last_event_id,
                projected_at = EXCLUDED.projected_at,
                status = EXCLUDED.status
            """,
            "snapshot:" + snapshotId,
            event.eventId(),
            event.occurredAt()
        );
    }

    /**
     * UPSERT générique avec contrôle de version (E1-06).
     * Si source_version >= entityVersion, l'événement est obsolète → ignoré.
     */
    private void upsertProjection(String table, ProjectionEvent event, String status) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(event.payload());
        } catch (Exception e) {
            log.error("[PROJECTION] Sérialisation payload impossible pour eventId={}", event.eventId());
            throw new RuntimeException(e);
        }

        // INSERT … ON CONFLICT avec contrôle d'ordre par entityVersion
        int updated = jdbc.update("""
            INSERT INTO %s (id, source_ref, source_version, last_event_id,
                projected_at, source_occurred_at, payload_raw, status)
            VALUES (gen_random_uuid(), ?, ?, ?, now(), ?, ?::jsonb, ?)
            ON CONFLICT (source_ref) DO UPDATE SET
                source_version = EXCLUDED.source_version,
                last_event_id = EXCLUDED.last_event_id,
                projected_at = EXCLUDED.projected_at,
                source_occurred_at = EXCLUDED.source_occurred_at,
                payload_raw = EXCLUDED.payload_raw,
                status = EXCLUDED.status
            WHERE %s.source_version < EXCLUDED.source_version
            """.formatted(table, table),
            event.entityRef(),
            event.entityVersion(),
            event.eventId(),
            event.occurredAt(),
            payloadJson,
            status
        );

        if (updated == 0) {
            log.debug("[PROJECTION] Ignoré (version obsolète) : table={} entityRef={} entityVersion={}",
                table, event.entityRef(), event.entityVersion());
        }
    }
}
