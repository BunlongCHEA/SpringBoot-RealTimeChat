package com.project.realtimechat.config;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	private static final Logger log = LoggerFactory.getLogger(JwtService.class);
	
	private static final String utcString = Instant.now().toString();
	
	@Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpirationInSeconds;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationInSeconds;
    
    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    // Extract expiration date from token
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    // Extracts token type (access or refresh)
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("token_type", String.class));
    }
    
    /**
     * Extracts a specific claim from a token
     * @param token The JWT token
     * @param claimsResolver Function to extract the desired claim
     * @return The extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extracts all claims from a token
     * @param token The JWT token
     * @return All claims in the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
        		.verifyWith(getSigningKey())
        		.build()
        		.parseSignedClaims(token)
        		.getPayload();
    }
    
    /**
     * Generates an access token for a user
     * @param userDetails The user details
     * @return The generated JWT access token
     */
    public String generateToken(UserDetails userDetails) {
    	Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "access");
        
        return createToken(claims, userDetails.getUsername(), jwtExpirationInSeconds);
    }
    
    /**
     * Generates a refresh token for a user
     * @param userDetails The user details
     * @return The generated JWT refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "refresh");
        
        return createToken(claims, userDetails.getUsername(), refreshExpirationInSeconds);
    }
    
    /**
     * Creates a token with the specified claims and subject
     * @param claims Claims to include in the token
     * @param subject The subject (usually username)
     * @param expirationInSeconds Token expiration time in seconds
     * @return The JWT token
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationInSeconds) {
        long now = System.currentTimeMillis();
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationInSeconds * 1000)) // Convert to milliseconds
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Validates a token for a specific user
     * @param token The JWT token
     * @param userDetails The user details
     * @return true if the token is valid, false otherwise
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates just the token without user details
     * @param token The JWT token
     * @return true if the token is valid, false otherwise
     */
    public Boolean validateToken(String token) {
        try {
            // Parse and verify signature
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
                
            // Check if token is expired
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if a token is expired
     * @param token The JWT token
     * @return true if the token is expired, false otherwise
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true; // Consider expired if there's an error
        }
    }
    
    /**
     * Checks if a token is a refresh token
     * @param token The JWT token
     * @return true if it's a refresh token, false otherwise
     */
    public boolean isRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            log.error("Error checking token type: {}", e.getMessage());
            return false;
        }
    }    
    
    // Get signing key from SecretKey
    private SecretKey getSigningKey() {
    	// For HMAC-SHA algorithms, the secret is used directly
    	// Keys.hmacShaKeyFor() returns a SecretKey specifically
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
