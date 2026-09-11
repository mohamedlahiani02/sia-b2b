package tn.sia.b2b.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private final StringRedisTemplate redis;
    private final int registerMaxRequests;
    private final long registerWindowMinutes;
    private final int loginMaxFailures;
    private final long loginLockoutMinutes;

    public RateLimitService(
            StringRedisTemplate redis,
            @Value("${app.rate-limit.register.max-requests:3}") int registerMaxRequests,
            @Value("${app.rate-limit.register.window-minutes:60}") long registerWindowMinutes,
            @Value("${app.rate-limit.login.max-failures:5}") int loginMaxFailures,
            @Value("${app.rate-limit.login.lockout-minutes:15}") long loginLockoutMinutes) {
        this.redis = redis;
        this.registerMaxRequests = registerMaxRequests;
        this.registerWindowMinutes = registerWindowMinutes;
        this.loginMaxFailures = loginMaxFailures;
        this.loginLockoutMinutes = loginLockoutMinutes;
    }

    public boolean isRegisterAllowed(String ip) {
        String key = "ratelimit:register:" + ip;
        return checkAndIncrement(key, registerMaxRequests, registerWindowMinutes, TimeUnit.MINUTES);
    }

    public boolean isLoginAllowed(String email) {
        String key = "ratelimit:login:" + email.toLowerCase();
        return checkAndIncrement(key, loginMaxFailures, loginLockoutMinutes, TimeUnit.MINUTES);
    }

    public void resetLoginAttempts(String email) {
        redis.delete("ratelimit:login:" + email.toLowerCase());
    }

    private boolean checkAndIncrement(String key, int max, long window, TimeUnit unit) {
        Long count = redis.opsForValue().increment(key);
        if (count == null) return true;
        if (count == 1) {
            redis.expire(key, window, unit);
        }
        return count <= max;
    }
}
