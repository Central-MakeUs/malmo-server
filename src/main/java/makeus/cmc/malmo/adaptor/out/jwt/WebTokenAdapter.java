package makeus.cmc.malmo.adaptor.out.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import makeus.cmc.malmo.application.port.out.member.GenerateWebTokenPort;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class WebTokenAdapter implements GenerateWebTokenPort {

    private final String secretKey;
    private final long accessTokenExpirationSeconds;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public WebTokenAdapter(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration-seconds}") long accessTokenExpirationSeconds,
            Clock clock
    ) {
        this.secretKey = secretKey;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.clock = clock;
    }

    @Override
    public String generateAccessToken(Long memberId, MemberRole memberRole) {
        Date issuedAt = Date.from(clock.instant());
        Date expiresAt = Date.from(clock.instant().plusSeconds(accessTokenExpirationSeconds));
        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .claim("role", memberRole.name())
                .claim("client", "WEB")
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hashRefreshToken(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
