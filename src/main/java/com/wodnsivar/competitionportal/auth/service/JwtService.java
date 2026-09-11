package com.wodnsivar.competitionportal.auth.service;

import com.wodnsivar.competitionportal.auth.security.UserPrincipal;
import com.wodnsivar.competitionportal.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final JwtConfig jwtConfig;

  public String generateToken(UserPrincipal userPrincipal) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + jwtConfig.getExpirationMs());

    return Jwts.builder()
        .subject(userPrincipal.getEmail())
        .claim("userId", userPrincipal.getId())
        .claim("role", userPrincipal.getRole().name())
        .claim("version", userPrincipal.getTokenVersion())
        .issuedAt(now)
        .expiration(expiration)
        .signWith(getSigningKey())
        .compact();
  }

  public String extractEmail(String token) {
    return extractAllClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, UserPrincipal userPrincipal) {
    String email = extractEmail(token);

    Integer version = extractAllClaims(token).get("version", Integer.class);
    return userPrincipal.isEnabled()
        && email.equals(userPrincipal.getEmail())
        && (version == null ? 0 : version) == userPrincipal.getTokenVersion()
        && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    Date expiration = extractAllClaims(token).getExpiration();
    return expiration.before(new Date());
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
