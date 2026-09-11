package tn.sia.b2b.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final JdbcTemplate jdbc;

    public AuditLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(UUID actorId, String action, String entity, String entityId,
                       Map<String, Object> diff, String ip) {
        try {
            String diffJson = diff != null ? toJson(diff) : null;
            jdbc.update(
                "INSERT INTO audit_log (id, actor_id, action, entity, entity_id, diff, ip, at) VALUES (?,?,?,?,?,?::jsonb,?,?)",
                UUID.randomUUID(), actorId, action, entity, entityId, diffJson, ip, Instant.now()
            );
        } catch (Exception e) {
            log.error("Failed to write audit_log: action={} entity={} entityId={}", action, entity, entityId, e);
        }
    }

    public void record(String action, String entity, String entityId) {
        record(null, action, entity, entityId, null, null);
    }

    private String toJson(Map<String, Object> map) {
        // Sérialisation JSON minimale sans dépendance externe dans ce service utilitaire
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val == null) sb.append("null");
            else if (val instanceof Number) sb.append(val);
            else if (val instanceof Boolean) sb.append(val);
            else sb.append("\"").append(val.toString().replace("\"", "\\\"")).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
