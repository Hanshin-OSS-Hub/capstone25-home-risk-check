package hanshin.home_risk_check.global.security;

import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;

@Component
public class JwtUtil {

    @Value("${jwt.private-key}")
    private RSAPrivateKey privateKey;

    @Value("${jwt.public-key}")
    private RSAPublicKey publicKey;

    private static final long ACCESS_TOKEN_EXPIRY  = 1000 * 60 * 30;
    private static final long REFRESH_TOKEN_EXPIRY = 1000 * 60 * 60 * 24 * 7;

    public String generateAccessToken(String email, String role) {
        return Jwts.builder()
                   .subject(email)
                   .claim("role", role)
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
                   .signWith(privateKey)
                   .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                   .subject(email)
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
                   .signWith(privateKey)
                   .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                   .verifyWith(publicKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}