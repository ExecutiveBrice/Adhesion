package com.wild.corp.adhesion.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PasswordResetRateLimiter {
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();
    private final int ipLimit;
    private final int addressLimit;
    private final long windowMillis;
    private final int maxTrackedKeys;
    private final Clock clock;

    @Autowired
    public PasswordResetRateLimiter(
            @Value("${adhesion.security.password-reset.rate-limit.ip:20}") int ipLimit,
            @Value("${adhesion.security.password-reset.rate-limit.address:5}") int addressLimit,
            @Value("${adhesion.security.password-reset.rate-limit.window-seconds:3600}") long windowSeconds,
            @Value("${adhesion.security.password-reset.rate-limit.max-tracked-keys:100000}") int maxTrackedKeys) {
        this(ipLimit, addressLimit, windowSeconds, maxTrackedKeys, Clock.systemUTC());
    }

    PasswordResetRateLimiter(int ipLimit, int addressLimit, long windowSeconds,
                             int maxTrackedKeys, Clock clock) {
        this.ipLimit = ipLimit;
        this.addressLimit = addressLimit;
        this.windowMillis = Math.multiplyExact(windowSeconds, 1_000L);
        this.maxTrackedKeys = maxTrackedKeys;
        this.clock = clock;
    }

    public boolean allow(String ipAddress, String addressHash) {
        long now = clock.millis();
        if ((requests.incrementAndGet() & 255) == 0) {
            windows.entrySet().removeIf(entry -> entry.getValue().expired(now, windowMillis));
        }
        boolean ipAllowed = acquire("ip:" + ipAddress, ipLimit, now);
        boolean addressAllowed = acquire("address:" + addressHash, addressLimit, now);
        return ipAllowed & addressAllowed;
    }

    private boolean acquire(String key, int limit, long now) {
        Window existing = windows.get(key);
        if (existing == null && windows.size() >= maxTrackedKeys) {
            return false;
        }
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        return window.acquire(limit, now, windowMillis);
    }

    private static final class Window {
        private long startedAt;
        private int count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }

        private synchronized boolean acquire(int limit, long now, long windowMillis) {
            if (expired(now, windowMillis)) {
                startedAt = now;
                count = 0;
            }
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }

        private synchronized boolean expired(long now, long windowMillis) {
            return now - startedAt >= windowMillis;
        }
    }
}
