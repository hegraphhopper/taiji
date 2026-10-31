package com.taiji.fund.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String DEMO_SECRET = "dev-only-fund-platform-jwt-secret-do-not-use-in-prod-2026";
    private static final int MIN_SECRET_LENGTH = 32;

    private final JwtProperties properties;
    private final Environment environment;

    private Key signingKey;

    @PostConstruct
    public void init() {
        String secret = properties.getSecret();
        if (secret == null || secret.trim().length() < MIN_SECRET_LENGTH) {
            if (isDemoProfile()) {
                secret = DEMO_SECRET;
                log.warn("JWT_SECRET 未配置或长度不足 {} 字节，demo 环境使用内置开发密钥，禁止用于生产", MIN_SECRET_LENGTH);
            } else {
                throw new IllegalStateException("JWT_SECRET 未配置或长度不足 " + MIN_SECRET_LENGTH + " 字节，prod 环境拒绝启动");
            }
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public GeneratedToken createToken(LoginUser user) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + properties.getExpireHours() * 3600_000L);
        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("uid", user.getId())
                .claim("roles", user.getRoles())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
        return new GeneratedToken(token, LocalDateTime.now().plusHours(properties.getExpireHours()));
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parse(token).get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return java.util.Collections.emptyList();
    }

    private boolean isDemoProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return true;
        }
        for (String profile : profiles) {
            if ("prod".equalsIgnoreCase(profile)) {
                return false;
            }
        }
        for (String profile : profiles) {
            if ("demo".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    @Getter
    @RequiredArgsConstructor
    public static class GeneratedToken {
        private final String token;
        private final LocalDateTime expiresAt;
    }
}
