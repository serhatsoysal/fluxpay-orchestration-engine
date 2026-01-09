package com.fluxpay.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String secret;
    private long expiration;
    private UUID userId;
    private UUID tenantId;
    private String role;

    @BeforeEach
    void setUp() {
        secret = "test-secret-key-that-is-at-least-64-characters-long-for-hmac-sha-512-algorithm";
        expiration = 3600000L;
        jwtTokenProvider = new JwtTokenProvider(secret, expiration);
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        role = "USER";
    }

    @Test
    void testCreateTokenWithoutSessionId() {
        String token = jwtTokenProvider.createToken(userId, tenantId, role);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        UUID extractedUserId = jwtTokenProvider.getUserId(token);
        UUID extractedTenantId = jwtTokenProvider.getTenantId(token);
        String extractedRole = jwtTokenProvider.getRole(token);
        
        assertEquals(userId, extractedUserId);
        assertEquals(tenantId, extractedTenantId);
        assertEquals(role, extractedRole);
    }

    @Test
    void testCreateTokenWithSessionId() {
        String sessionId = UUID.randomUUID().toString();
        String token = jwtTokenProvider.createToken(userId, tenantId, role, sessionId);
        
        assertNotNull(token);
        String extractedSessionId = jwtTokenProvider.getSessionId(token);
        assertEquals(sessionId, extractedSessionId);
    }

    @Test
    void testValidateTokenWithValidToken() {
        String token = jwtTokenProvider.createToken(userId, tenantId, role);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testValidateTokenWithInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void testValidateTokenWithExpiredToken() {
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(secret, -1000L);
        String token = shortExpirationProvider.createToken(userId, tenantId, role);
        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testValidateTokenWithNullToken() {
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void testGetUserId() {
        String token = jwtTokenProvider.createToken(userId, tenantId, role);
        UUID extractedUserId = jwtTokenProvider.getUserId(token);
        assertEquals(userId, extractedUserId);
    }

    @Test
    void testGetUserIdWithInvalidToken() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getUserId("invalid.token");
        });
    }

    @Test
    void testGetTenantId() {
        String token = jwtTokenProvider.createToken(userId, tenantId, role);
        UUID extractedTenantId = jwtTokenProvider.getTenantId(token);
        assertEquals(tenantId, extractedTenantId);
    }

    @Test
    void testGetTenantIdWithInvalidToken() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getTenantId("invalid.token");
        });
    }

    @Test
    void testGetRole() {
        String token = jwtTokenProvider.createToken(userId, tenantId, role);
        String extractedRole = jwtTokenProvider.getRole(token);
        assertEquals(role, extractedRole);
    }

    @Test
    void testGetRoleWithInvalidToken() {
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getRole("invalid.token");
        });
    }

    @Test
    void testGetSessionId() {
        String sessionId = UUID.randomUUID().toString();
        String token = jwtTokenProvider.createToken(userId, tenantId, role, sessionId);
        String extractedSessionId = jwtTokenProvider.getSessionId(token);
        assertEquals(sessionId, extractedSessionId);
    }

    @Test
    void testGetSessionIdWithoutSessionId() {
        String token = jwtTokenProvider.createToken(userId, tenantId, role);
        String extractedSessionId = jwtTokenProvider.getSessionId(token);
        assertNull(extractedSessionId);
    }

    @Test
    void testGetExpirationDate() {
        String token = jwtTokenProvider.createToken(userId, tenantId, role);
        Date expirationDate = jwtTokenProvider.getExpirationDate(token);
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void testGetUserIdWithEmptySubject() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String tokenWithEmptySubject = Jwts.builder()
                .subject("")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
        
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getUserId(tokenWithEmptySubject);
        });
    }

    @Test
    void testGetTenantIdWithEmptyTenantId() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String tokenWithEmptyTenantId = Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", "")
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
        
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getTenantId(tokenWithEmptyTenantId);
        });
    }

    @Test
    void testGetRoleWithEmptyRole() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String tokenWithEmptyRole = Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("role", "")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
        
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getRole(tokenWithEmptyRole);
        });
    }

    @Test
    void testGetUserIdWithInvalidUuidFormat() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String tokenWithInvalidUuid = Jwts.builder()
                .subject("invalid-uuid-format")
                .claim("tenantId", tenantId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
        
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getUserId(tokenWithInvalidUuid);
        });
    }

    @Test
    void testGetTenantIdWithInvalidUuidFormat() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String tokenWithInvalidUuid = Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", "invalid-uuid-format")
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
        
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getTenantId(tokenWithInvalidUuid);
        });
    }

    @Test
    void testGetUserIdWithNullSubject() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String tokenWithNullSubject = Jwts.builder()
                .claim("tenantId", tenantId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
        
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getUserId(tokenWithNullSubject);
        });
    }

    @Test
    void testGetTenantIdWithNullTenantId() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String tokenWithNullTenantId = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
        
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getTenantId(tokenWithNullTenantId);
        });
    }

    @Test
    void testGetRoleWithNullRole() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String tokenWithNullRole = Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
        
        assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.getRole(tokenWithNullRole);
        });
    }

    @Test
    void testValidateTokenWithWrongSecret() {
        String token = jwtTokenProvider.createToken(userId, tenantId, role);
        JwtTokenProvider differentProvider = new JwtTokenProvider("different-secret-key-that-is-at-least-64-characters-long-for-hmac-sha-512-algorithm", expiration);
        assertFalse(differentProvider.validateToken(token));
    }
}

