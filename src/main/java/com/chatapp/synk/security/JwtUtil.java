package com.chatapp.synk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private final Key secretKey;

    @Autowired
    //@Autowired here tells Spring:
    //"When creating a JwtUtil bean, call this constructor and inject its parameters automatically."
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generate JWT token with claims
    public String generateToken(Map<String, Object> claims, String username) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour expiry
                .signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

    // Extract any claim securely (with signature verification)
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseToken(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token) {
        try {
            // This call checks both signature and expiration based on 'exp' claim.
            Claims claims = parseToken(token);
            // Check not expired (defensive: handles missing expiration as invalid)
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // Invalid token: bad signature, malformed, expired, etc.
            logger.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Parse token to verify signature
     *
     * @param token JWT token to parse
     * @return Claims object containing the token's claims
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)   // This verifies the signature!
                    .getBody();
        } catch (JwtException e) {
            logger.error("Invalid JWT signature or token! exception msg:{}", e);
            throw new RuntimeException("Invalid JWT signature or token!", e); // Force rejection on parse/signature error
        }
    }

    /**
     * check if token is expired
     *
     * @param token token JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public boolean validateToken(String token, String username) {
        String extractedUsername = extractClaim(token, Claims::getSubject);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        String extractedUsername = extractClaim(token, Claims::getSubject);
        return extractedUsername;
    }

    // Custom claims  extractors setted in AuthController authenticate.
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }


    public String extractName(String token) {
        return extractClaim(token, claims -> claims.get("name", String.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractProfilePictureUrl(String token) {
        return extractClaim(token, claims -> claims.get("profilePictureUrl", String.class));
    }

    public String extractId(String token) {
        return extractClaim(token, claims -> claims.get("id", String.class));
    }
}
