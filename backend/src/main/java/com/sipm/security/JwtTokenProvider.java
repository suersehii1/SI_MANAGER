package com.sipm.security;

import com.sipm.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProvider(JwtProperties props) {
        byte[] bytes = Decoders.BASE64.decode(props.secret());
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes after Base64 decoding");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationSeconds = props.expirationSeconds() > 0 ? props.expirationSeconds() : 28800;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String createToken(AppUserPrincipal principal) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getId())
                .claim("roles", principal.getRoles())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationSeconds * 1000L))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Returns the subject (username) if the token is valid, otherwise empty. */
    public Optional<String> validateAndGetUsername(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
