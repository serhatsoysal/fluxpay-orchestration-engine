package com.fluxpay.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long validityInMilliseconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
    }

    public String createToken(UUID userId, UUID tenantId, String role) {
        return createToken(userId, tenantId, role, null);
    }

    public String createToken(UUID userId, UUID tenantId, String role, String sessionId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("role", role)
                .claim("sessionId", sessionId)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID getUserId(String token) {
        String subject = getClaims(token).getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Token subject is missing or empty");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID format in token", e);
        }
    }

    public UUID getTenantId(String token) {
        String tenantId = getClaims(token).get("tenantId", String.class);
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Token tenantId is missing or empty");
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tenant ID format in token", e);
        }
    }

    public String getRole(String token) {
        String role = getClaims(token).get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Token role is missing or empty");
        }
        return role;
    }

    public String getSessionId(String token) {
        return getClaims(token).get("sessionId", String.class);
    }

    public Date getExpirationDate(String token) {
        return getClaims(token).getExpiration();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

