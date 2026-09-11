package tn.sia.b2b.ingestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Accumule les tranches d'un snapshot (E1-08).
 *
 * Un snapshot stock est potentiellement trop volumineux pour un seul message Kafka.
 * YME le découpe en N chunks, chacun étant un événement distinct avec :
 *   payload.snapshotId    — identifiant du snapshot global
 *   payload.chunkIndex    — index de la tranche (0-based)
 *   payload.totalChunks   — nombre total de tranches
 *   payload.items         — articles contenus dans cette tranche
 *
 * Quand toutes les tranches sont reçues, le snapshot est prêt à être appliqué.
 * On stocke l'état de complétion dans Redis avec TTL de 1h.
 */
@Component
public class SnapshotChunkAccumulator {

    private static final Logger log = LoggerFactory.getLogger(SnapshotChunkAccumulator.class);
    private static final Duration CHUNK_TTL = Duration.ofHours(1);
    private static final String PREFIX = "snapshot:";

    private final StringRedisTemplate redis;

    public SnapshotChunkAccumulator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Enregistre une tranche et retourne true si le snapshot est complet.
     */
    public boolean recordChunk(String snapshotId, int chunkIndex, int totalChunks, String payloadJson) {
        String chunkKey = PREFIX + snapshotId + ":chunk:" + chunkIndex;
        String counterKey = PREFIX + snapshotId + ":count";
        String totalKey = PREFIX + snapshotId + ":total";

        redis.opsForValue().set(chunkKey, payloadJson, CHUNK_TTL);
        redis.opsForValue().set(totalKey, String.valueOf(totalChunks), CHUNK_TTL);

        Long received = redis.opsForValue().increment(counterKey);
        redis.expire(counterKey, CHUNK_TTL);

        if (received == null) return false;

        boolean complete = received >= totalChunks;
        if (complete) {
            log.info("[SNAPSHOT] snapshotId={} complet ({}/{} tranches reçues)", snapshotId, received, totalChunks);
        } else {
            log.debug("[SNAPSHOT] snapshotId={} {}/{} tranches", snapshotId, received, totalChunks);
        }
        return complete;
    }

    /**
     * Lit une tranche depuis Redis.
     */
    public Optional<String> getChunk(String snapshotId, int chunkIndex) {
        return Optional.ofNullable(redis.opsForValue().get(PREFIX + snapshotId + ":chunk:" + chunkIndex));
    }

    /**
     * Nombre total de tranches attendues.
     */
    public Optional<Integer> getTotalChunks(String snapshotId) {
        String v = redis.opsForValue().get(PREFIX + snapshotId + ":total");
        return v == null ? Optional.empty() : Optional.of(Integer.parseInt(v));
    }

    public void cleanup(String snapshotId, int totalChunks) {
        redis.delete(PREFIX + snapshotId + ":count");
        redis.delete(PREFIX + snapshotId + ":total");
        for (int i = 0; i < totalChunks; i++) {
            redis.delete(PREFIX + snapshotId + ":chunk:" + i);
        }
    }
}
