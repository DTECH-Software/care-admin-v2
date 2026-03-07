/**
 * User: Himal_J
 * Date: 4/22/2025
 * Time: 11:12 AM
 * <p>
 */

package com.dtech.admin.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Log4j2
@Component
public class JwtUtil {

    @Value("${jwt.token.secret.key}")
    private String SECRET_KEY;

    @Value("${jwt.token.access.exp.time}")
    private long ACCESS_EXP_TIME;

    @Value("${jwt.token.refresh.exp.time}")
    private long REFRESH_EXP_TIME;

    // Generate Access Token
    public String generateAccessToken(String username) {
        log.info("Generating access token for {}", username);
        return buildToken(username, ACCESS_EXP_TIME,"access");
    }

    // Generate Refresh Token (no roles usually)
    public String generateRefreshToken(String username) {
        log.info("Generating refresh token for {}", username);
        return buildToken(username,  REFRESH_EXP_TIME,"refresh");
    }

    // Common token builder
    public String buildToken(String username,  long expirationTime,String tokenType) {
        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim("type", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignInKey());

        return builder.compact();
    }

    // Extract username
    public String extractUsername(String token) {
        log.info("Extracting username from token: {}", token);
        return extractClaims(token).getSubject();
    }

    // Extract all claims
    public Claims extractClaims(String token) {
        log.info("Extracting claims from token: {}", token);
        try {
            return Jwts.parser()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Token is expired, but returning claims anyway");
            return e.getClaims();
        }
    }

    // Validate token
    public boolean validateToken(String token,String requestUsername,String username) {
        try {
            log.info("Validating token...");
            Claims claims = Jwts.parser()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getBody();

            boolean isExpired = claims.getExpiration().before(new Date());
            if (isExpired || !username.equals(requestUsername) ) {
                log.warn("Token expired at {}", claims.getExpiration());
                return false;
            }

            log.info("Token is valid.");
            return true;

        } catch (ExpiredJwtException e) {
            log.error("Token expired", e);
        } catch (Exception e) {
            log.error("Token invalid", e);
        }
        return false;
    }

    // Helper to get the signing key
    private SecretKey getSignInKey() {
        log.debug("Getting sign-in key...  {}",SECRET_KEY);
        return new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}

