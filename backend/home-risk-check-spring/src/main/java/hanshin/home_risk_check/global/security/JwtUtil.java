package hanshin.home_risk_check.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.private-key}")
    private RSAPrivateKey privateKey;

    @Value("${jwt.public-key}")
    private RSAPublicKey publicKey;

    @Value("${jwt.access-token-expiration}")
    private long ACCESS_TOKEN_EXPIRATION;

    @Value("${jwt.refresh-token-expiration}")
    private long REFRESH_TOKEN_EXPIRATION;

    public String generateAccessToken(Long userId, String role) {
        return Jwts.builder()
                   .subject(String.valueOf(userId))
                   .claim("role", role)
                   .claim("type", TokenType.ACCESS.name())
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                   .signWith(privateKey)
                   .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                   .subject(String.valueOf(userId))
                   .claim("type", TokenType.REFRESH.name())
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
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

    public TokenValidationResult validate(String token) {
        try {
            parseClaims(token);
            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.EXPIRED;
        } catch (SignatureException e) {
            return TokenValidationResult.INVALID_SIGNATURE;
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            return TokenValidationResult.MALFORMED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenValidationResult.INVALID;
        }
    }

    public boolean isExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean isAccessToken(String token) {
        return parseClaims(token).get("type", String.class).equals(TokenType.ACCESS.name());
    }

    public boolean isRefreshToken(String token) {
        return parseClaims(token).get("type", String.class).equals(TokenType.REFRESH.name());
    }

    public enum TokenValidationResult {
        VALID,
        EXPIRED,
        INVALID_SIGNATURE,
        MALFORMED,
        INVALID
    }

    public enum TokenType {
        ACCESS,
        REFRESH
    }
}