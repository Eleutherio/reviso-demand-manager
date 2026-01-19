package com.guilherme.reviso_demand_manager.infra;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    
    private final Map<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_SIGNUP_ATTEMPTS = 3;
    private static final long WINDOW_SECONDS = 60;

    public boolean isAllowed(String key) {
        return isAllowed(key, MAX_LOGIN_ATTEMPTS);
    }

    public boolean isAllowed(String key, int maxAttempts) {
        var entry = attempts.get(key);
        var now = Instant.now();
        
        if (entry == null) {
            attempts.put(key, new RateLimitEntry(1, now));
            return true;
        }
        
        if (now.getEpochSecond() - entry.firstAttemptTime.getEpochSecond() > WINDOW_SECONDS) {
            attempts.put(key, new RateLimitEntry(1, now));
            return true;
        }
        
        if (entry.count >= maxAttempts) {
            return false;
        }
        
        entry.count++;
        return true;
    }

    public boolean isSignupAllowed(String key) {
        return isAllowed("signup:" + key, MAX_SIGNUP_ATTEMPTS);
    }
    
    public void reset(String key) {
        attempts.remove(key);
    }
    
    private static class RateLimitEntry {
        int count;
        Instant firstAttemptTime;
        
        RateLimitEntry(int count, Instant firstAttemptTime) {
            this.count = count;
            this.firstAttemptTime = firstAttemptTime;
        }
    }
}
