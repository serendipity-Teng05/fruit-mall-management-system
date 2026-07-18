package com.example.fruitmallmanagementsystem.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    private final SecretKey signingKey;
    private final long expireTime;

    public JwtUtils(@Value("${app.jwt.secret}") String secretKey,
                    @Value("${app.jwt.expire-millis}") long expireTime) {
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expireTime = expireTime;
    }

    public String generateToken(UserTokenClaims user) {
        return Jwts.builder()
                .subject(user.username())
                .claim("uid", user.userId())
                .claim("ver", user.tokenVersion())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public long getExpireTime() {
        return expireTime;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? null : claims.getSubject();
    }

    public Integer getTokenVersion(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? null : claims.get("ver", Integer.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ignored) {
            return null;
        }
    }

    public record UserTokenClaims(Long userId, String username, Integer tokenVersion) {}
}
