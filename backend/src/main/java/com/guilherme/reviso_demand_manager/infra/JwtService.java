package com.guilherme.reviso_demand_manager.infra;

import com.guilherme.reviso_demand_manager.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationHours;

    public JwtService(
            @Value("${jwt.secret:change-me-in-production-use-a-long-random-secret-key-at-least-256-bits}") String secret,
            @Value("${jwt.expiration-hours:24}") long expirationHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    public String generateToken(UUID userId, String email, UserRole role, UUID companyId, UUID agencyId) {
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationHours, ChronoUnit.HOURS);

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey);

        if (companyId != null) {
            builder.claim("companyId", companyId.toString());
        }
        if (agencyId != null) {
            builder.claim("agencyId", agencyId.toString());
        }

        return builder.compact();
    }

    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public UserRole extractRole(Claims claims) {
        String role = claims.get("role", String.class);
        return UserRole.valueOf(role);
    }

    public UUID extractCompanyId(Claims claims) {
        String companyIdStr = claims.get("companyId", String.class);
        return companyIdStr != null ? UUID.fromString(companyIdStr) : null;
    }

    public UUID extractAgencyId(Claims claims) {
        String agencyIdStr = claims.get("agencyId", String.class);
        return agencyIdStr != null ? UUID.fromString(agencyIdStr) : null;
    }
}
