package com.productapp.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    // FIX: cache the key — previously getKey() decoded Base64 and created a new
    // SecretKey object on EVERY token validation (i.e. every authenticated request).
    private SecretKey cachedKey;

    @PostConstruct
    private void initKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.cachedKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String subject, String role) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(cachedKey)
                .compact();
    }

    public String generateToken(String subject, String role, String name) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("name", name)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(cachedKey)
                .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(cachedKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
