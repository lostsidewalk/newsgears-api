package com.lostsidewalk.buffy.app.token;

import com.lostsidewalk.buffy.app.audit.TokenValidationException;
import com.lostsidewalk.buffy.app.model.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import static io.jsonwebtoken.SignatureAlgorithm.HS256;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

@Service
@Slf4j
public class TokenService {

    @Value("${token.service.secret}")
    private String secretKey;

    @SuppressWarnings("unused")
    public interface JwtUtil {
        String extractUsername();
        Date extractExpiration();
        String extractValidationClaim();
        Boolean isTokenValid();
        Boolean isTokenExpired();
        void requireNonExpired() throws TokenValidationException;
        void validateToken() throws TokenValidationException;
    }

    public JwtUtil instanceFor(TokenType tokenType, String token) throws TokenValidationException {

        final Claims claims;
        try {
            claims = Jwts.parser().requireAudience(tokenType.name()).setSigningKey(this.secretKey).parseClaimsJws(token).getBody();
        } catch (Exception e) {
            throw new TokenValidationException("Unable to parse token due to: " + getRootCauseMessage(e));
        }

        JwtUtil i = new JwtUtil() {

            @Override
            public String extractUsername() {
                return extractClaim(Claims::getSubject);
            }

            @Override
            public Date extractExpiration() {
                return extractClaim(Claims::getExpiration);
            }

            @Override
            public String extractValidationClaim() {
                return extractClaim(claims -> claims.get(tokenType.tokenName, String.class));
            }

            private <T> T extractClaim(Function<Claims, T> claimsResolver) {
                return claimsResolver.apply(claims);
            }

            @Override
            public Boolean isTokenValid() {
                return !isNull(claims);
            }

            @Override
            public Boolean isTokenExpired() {
                return extractExpiration().before(new Date());
            }

            @Override
            public void requireNonExpired() throws TokenValidationException {
                if (isTokenExpired()) {
                    throw new TokenValidationException("Token is expired");
                }
            }

            @Override
            public void validateToken() throws TokenValidationException {
                if (!isTokenValid()) {
                    throw new TokenValidationException("Not a valid JWT token");
                }
            }
        };

        i.validateToken();

        return i;
    }

    //
    // methods to query the token
    //
    public String generateToken(Map<String, Object> claims, String subject, TokenType tokenType) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(currentTimeMillis()))
                .setExpiration(tokenType.expirationBuilder.apply(currentTimeMillis()))
                .setAudience(tokenType.name())
                .signWith(HS256, this.secretKey).compact();
    }
}