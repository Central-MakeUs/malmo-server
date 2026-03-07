package makeus.cmc.malmo.adaptor.out.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.out.member.GenerateTokenPort;
import makeus.cmc.malmo.application.port.out.member.ValidateTokenPort;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAdaptor implements GenerateTokenPort, ValidateTokenPort {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration-seconds}")
    private long accessTokenExpirationSeconds;

    @Value("${jwt.refresh-token-expiration-seconds}")
    private long refreshTokenExpirationSeconds;

    @Override
    public TokenInfo generateToken(Long memberId, MemberRole memberRole) {
        Date now = new Date();
        Date accessTokenExpiry = new Date(now.getTime() + accessTokenExpirationSeconds * 1000);
        Date refreshTokenExpiry = new Date(now.getTime() + refreshTokenExpirationSeconds * 1000);

        String accessToken = createToken(memberId, memberRole.name(), accessTokenExpiry);
        String refreshToken = createToken(memberId, memberRole.name(), refreshTokenExpiry);

        return TokenInfo.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getMemberIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    @Override
    public String getMemberRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    private String createToken(Long memberId, String role, Date expiry) {
        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .claim("role", role)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}
