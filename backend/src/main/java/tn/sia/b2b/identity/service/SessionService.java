package tn.sia.b2b.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user:sessions:";

    private final StringRedisTemplate redis;
    private final long ttlHours;

    public SessionService(StringRedisTemplate redis,
                          @Value("${app.session.ttl-hours:8}") long ttlHours) {
        this.redis = redis;
        this.ttlHours = ttlHours;
    }

    public String createSession(UUID userId, String role, String customerSourceRef) {
        String token = UUID.randomUUID().toString();
        String key = SESSION_PREFIX + token;
        String value = userId + ":" + role + ":" + (customerSourceRef != null ? customerSourceRef : "");

        redis.opsForValue().set(key, value, ttlHours, TimeUnit.HOURS);

        // Suivre les sessions actives de l'utilisateur pour révocation en masse
        redis.opsForSet().add(USER_SESSIONS_PREFIX + userId, token);
        redis.expire(USER_SESSIONS_PREFIX + userId, ttlHours, TimeUnit.HOURS);

        return token;
    }

    public SessionData getSession(String token) {
        String value = redis.opsForValue().get(SESSION_PREFIX + token);
        if (value == null) return null;
        String[] parts = value.split(":", 3);
        if (parts.length < 2) return null;
        String customerRef = parts.length == 3 && !parts[2].isBlank() ? parts[2] : null;
        return new SessionData(UUID.fromString(parts[0]), parts[1], customerRef);
    }

    public void invalidateSession(String token) {
        String value = redis.opsForValue().get(SESSION_PREFIX + token);
        if (value != null) {
            String userId = value.split(":", 2)[0];
            redis.delete(SESSION_PREFIX + token);
            redis.opsForSet().remove(USER_SESSIONS_PREFIX + userId, token);
        }
    }

    public void invalidateAllUserSessions(UUID userId) {
        Set<String> tokens = redis.opsForSet().members(USER_SESSIONS_PREFIX + userId);
        if (tokens != null) {
            tokens.forEach(t -> redis.delete(SESSION_PREFIX + t));
        }
        redis.delete(USER_SESSIONS_PREFIX + userId);
    }

    public record SessionData(UUID userId, String role, String customerSourceRef) {}
}
