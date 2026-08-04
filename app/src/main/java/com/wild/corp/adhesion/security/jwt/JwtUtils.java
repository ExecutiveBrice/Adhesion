package com.wild.corp.adhesion.security.jwt;

import com.wild.corp.adhesion.models.UserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  private final SecretKey signingKey;
  private final long jwtExpirationMs;
  private final Clock clock;

  public JwtUtils(@Value("${server.app.jwtSecret}") String jwtSecret,
                  @Value("${server.app.jwtExpirationMs}") long jwtExpirationMs) {
    this(jwtSecret, jwtExpirationMs, Clock.systemUTC());
  }

  JwtUtils(String jwtSecret, long jwtExpirationMs, Clock clock) {
    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    this.jwtExpirationMs = jwtExpirationMs;
    this.clock = clock;
  }

  public String generateJwtToken(Authentication authentication) {

    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

    Date issuedAt = Date.from(clock.instant());
    return Jwts.builder().subject(userPrincipal.getUsername()).issuedAt(issuedAt)
        .expiration(new Date(issuedAt.getTime() + jwtExpirationMs)).signWith(signingKey)
        .compact();
  }

  public String getUserNameFromJwtToken(String token) {
    return parser().parseSignedClaims(token).getPayload().getSubject();
  }

  public boolean validateJwtToken(String authToken) {
    try {
      parser().parseSignedClaims(authToken);
      return true;
    } catch (SignatureException e) {
      logger.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    }

    return false;
  }

  private JwtParser parser() {
    return Jwts.parser().verifyWith(signingKey).clock(() -> Date.from(clock.instant())).build();
  }
}
