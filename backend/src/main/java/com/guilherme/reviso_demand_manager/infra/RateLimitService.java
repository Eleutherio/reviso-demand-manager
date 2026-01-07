package com.guilherme.reviso_demand_manager.infra;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço simples de rate limiting baseado em memória.
 * Implementa algoritmo de janela deslizante para limitar tentativas de login.
 */
@Service
public class RateLimitService {
    
    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60; // 1 minuto

    public boolean isAllowed(String key) {
        LoginAttempt attempt = attempts.get(key);
        Instant now = Instant.now();
        
        if (attempt == null) {
            attempts.put(key, new LoginAttempt(1, now));
            return true;
        }
        
        // Se janela expirou, reseta
        if (now.getEpochSecond() - attempt.firstAttemptTime.getEpochSecond() > WINDOW_SECONDS) {
            attempts.put(key, new LoginAttempt(1, now));
            return true;
        }
        
        // Incrementa contador
        if (attempt.count >= MAX_ATTEMPTS) {
            return false;
        }
        
        attempt.count++;
        return true;
    }
    
    public void reset(String key) {
        attempts.remove(key);
    }
    
    private static class LoginAttempt {
        int count;
        Instant firstAttemptTime;
        
        LoginAttempt(int count, Instant firstAttemptTime) {
            this.count = count;
            this.firstAttemptTime = firstAttemptTime;
        }
    }
}
