package com.propertyrental.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class JWTUtil {
    private static final String SECRET_KEY = "YourSuperSecretKeyYourSuperSecretKey"; // Must be 32+ chars
    private static final long EXPIRATION_TIME = 86400000; // 1 day in milliseconds

    private static Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // Generate token with additional claims (user_id and role)
    public static String generateToken(String email, String userId, String role) {
        return Jwts.builder()
                .setSubject(email) // Set email as the subject
                .claim("user_id", userId)  // Add user_id as a custom claim
                .claim("role", role)        // Add role as a custom claim
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate and retrieve the claims from the token
    public static Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    // Retrieve the email from the token
    public static String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return (claims != null) ? claims.getSubject() : null;
    }

    // Retrieve the user_id from the token
    public static String getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return (claims != null) ? claims.get("user_id", String.class) : null;
    }

    // Retrieve the role from the token
    public static String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return (claims != null) ? claims.get("role", String.class) : null;
    }
}