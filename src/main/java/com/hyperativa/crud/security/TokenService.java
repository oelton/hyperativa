package com.hyperativa.crud.security;

import com.hyperativa.crud.domain.model.User;
import com.hyperativa.crud.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration}")
    private Long expiration;

    @Value("${api.security.token.issuer}")
    private String issuer;

    @Value("${api.security.token.audience}")
    private String audience;

    @PostConstruct
    public void validateConfiguration() {
        if (secret == null || secret.length() < 32) {
            log.warn("JWT_SECRET deve ter no mínimo 32 caracteres (256 bits) para HS256!");
        }
        if (secret.equals("X7kP9mN2vQ8rT4wY1zA5bC3dE6fG0hJ2iL5nO7pR9sU2tV4xZ6")) {
            log.warn("JWT_SECRET usando valor padrão! Configure JWT_SECRET em produção!");
        }
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        
        if (userDetails instanceof User user) {
            claims.put("userId", ( user).getId());
        }
        
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
            throw new TokenException("Token expirado", e);
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Token JWT malformado: {}", e.getMessage());
            throw new TokenException("Token inválido", e);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Assinatura JWT inválida: {}", e.getMessage());
            throw new TokenException("Token com assinatura inválida", e);
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("Token JWT não suportado: {}", e.getMessage());
            throw new TokenException("Token não suportado", e);
        } catch (IllegalArgumentException e) {
            log.warn("Token JWT vazio ou nulo: {}", e.getMessage());
            throw new TokenException("Token vazio", e);
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
