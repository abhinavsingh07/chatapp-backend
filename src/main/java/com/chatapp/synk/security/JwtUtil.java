package com.chatapp.synk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.chatapp.synk.exceptionHandler.InvalidTokenException;

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
    // @Autowired here tells Spring:
    // "When creating a JwtUtil bean, call this constructor and inject its
    // parameters automatically."
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generate JWT token with claims
    public String generateAccessToken(Map<String, Object> claims, String username) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30)) // 1 minute expiry
                .signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

     // Generate Refresh Token (longer expiry: 7 days)
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // 7 days expiry
                .signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }



    /**
     * Parse token to verify signature
     *
     * @param token JWT token to parse
     * @return Claims object containing the token's claims
     */

    private Claims parseToken(String token) {
        // Cheap CPU operation: Fast-fail if the token format is obviously garbage
        if (token == null || token.split("\\.").length != 3) {
            throw new InvalidTokenException("Malformed token structural format");
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token) // This verifies the signature! cpu heavy operation, so do it once and reuse claims for all extractions.
                    .getBody();
        } catch (ExpiredJwtException e) {
            logger.error("Token has expired msg:{}", e.getMessage());
            throw new InvalidTokenException("Token has expired", e);
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid JWT signature or token! exception msg:{}", e.getMessage());
            throw new InvalidTokenException("Invalid token signature!", e); // Force rejection on parse/signature error
        }
    }

    /**
     * Parse token once and get Claims object.
     * Use this when you need to extract multiple claims from the same token to avoid parsing multiple times.
     * 
     * @param token JWT token to parse
     * @return Claims object if valid, throws exception otherwise
     */
    public Claims getTokenClaims(String token) {
        return parseToken(token);
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

    // Overloaded method to validate token directly from Claims (signature already verified)
    public boolean isTokenValid(Claims claims) {
        try {
            // Signature is already verified when we obtained the Claims object
            // Just check if token is not expired
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (Exception e) {
            logger.error("Token validity check failed: {}", e.getMessage());
            return false;
        }
    }

        // Validate Refresh Token
    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Refresh token validation failed: {}", e.getMessage());
            return false;
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

    // Overloaded method to check if token is expired directly from Claims
    public boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    public boolean validateToken(String token, String username) {
        String extractedUsername = extractClaim(token, Claims::getSubject);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // Overloaded method to validate token directly with Claims
    public boolean validateToken(Claims claims, String username) {
        String extractedUsername = claims.getSubject();
        return (extractedUsername.equals(username) && !isTokenExpired(claims));
    }

    public String extractUsername(String token) {
        String extractedUsername = extractClaim(token, Claims::getSubject);
        return extractedUsername;
    }

    // Overloaded method to extract username directly from Claims
    public String extractUsername(Claims claims) {
        return claims.getSubject();
    }

    // Custom claims  extractors setted in AuthController authenticate.
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    // Overloaded method to extract roles directly from Claims
    public List<String> extractRoles(Claims claims) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        return roles != null ? roles : List.of();
    }

    public String extractName(String token) {
        return extractClaim(token, claims -> claims.get("name", String.class));
    }

    // Overloaded method to extract name directly from Claims
    public String extractName(Claims claims) {
        return claims.get("name", String.class);
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    // Overloaded method to extract email directly from Claims
    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public String extractProfilePictureUrl(String token) {
        return extractClaim(token, claims -> claims.get("profilePictureUrl", String.class));
    }

    // Overloaded method to extract profile picture URL directly from Claims
    public String extractProfilePictureUrl(Claims claims) {
        return claims.get("profilePictureUrl", String.class);
    }

    public String extractId(String token) {
        return extractClaim(token, claims -> claims.get("id", String.class));
    }

    // Overloaded method to extract ID directly from Claims
    public String extractId(Claims claims) {
        return claims.get("id", String.class);
    }
}
