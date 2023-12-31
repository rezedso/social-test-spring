package com.social.test.security.services;

import com.social.test.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.Date;

@Component
public class JwtService {
    @Value("${ignacio.app.jwtSecret}")
    private String jwtSecret;

    @Value("${ignacio.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${ignacio.app.jwtCookieName}")
    private String jwtCookie;

    @Value("${ignacio.app.jwtRefreshCookieName}")
    private String jwtRefreshCookie;

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    public ResponseCookie generateJwtCookie(UserDetailsImpl userDetails) {
        String jwt = generateJwtFromUsername(userDetails.getUsername());
        return generateCookie(jwtCookie, jwt, "/");
    }

    public ResponseCookie generateJwtCookie(User user) {
        String jwt = generateJwtFromUsername(user.getUsername());
        return generateCookie(jwtCookie, jwt, "/");
    }

    public ResponseCookie generateJwtRefreshCookie(String refreshToken) {
        return generateCookie(jwtRefreshCookie, refreshToken, "/auth/refreshtoken");
    }

    public ResponseCookie getCleanJwtCookie() {
        return ResponseCookie.from(jwtCookie, null).path("/").build();
    }

    public ResponseCookie getCleanJwtRefreshCookie() {
        return ResponseCookie.from(jwtRefreshCookie, null).path("/auth/refreshtoken").build();
    }

    public String getJwtFromCookie(HttpServletRequest request) {
        return getCookieValueByName(request, jwtCookie);
    }

    public String getJwtRefreshFromCookie(HttpServletRequest request) {
        return getCookieValueByName(request, jwtRefreshCookie);
    }

    public String getCookieValueByName(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        return cookie != null ? cookie.getValue() : null;
    }

    public ResponseCookie generateCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .path(path).httpOnly(true).maxAge(24 * 60 * 60).build();
    }

    public String generateJwtFromUsername(String username) {
        return Jwts
                .builder()
                .signWith(key(), SignatureAlgorithm.HS256)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + jwtExpirationMs))
                .compact();
    }

    public String getUsernameFromJwt(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    public Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwt(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(token);
            return true;
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
}
