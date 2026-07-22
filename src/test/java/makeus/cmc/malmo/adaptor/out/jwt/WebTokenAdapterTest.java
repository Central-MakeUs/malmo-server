package makeus.cmc.malmo.adaptor.out.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class WebTokenAdapterTest {

    private static final String SECRET = "test-jwt-secret-key-for-testing-only";

    private final WebTokenAdapter adapter = new WebTokenAdapter(
            SECRET,
            3600,
            Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void accessTokenRemainsCompatibleWithExistingJwtAuthenticationClaims() {
        String token = adapter.generateAccessToken(10L, MemberRole.MEMBER);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .setClock(() -> Date.from(Instant.parse("2026-07-20T00:30:00Z")))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getSubject()).isEqualTo("10");
        assertThat(claims.get("role", String.class)).isEqualTo("MEMBER");
        assertThat(claims.get("client", String.class)).isEqualTo("WEB");
    }

    @Test
    void refreshTokensAreOpaqueAndHashedBeforePersistence() {
        String refreshToken = adapter.generateRefreshToken();

        assertThat(refreshToken).doesNotContain(".");
        assertThat(adapter.hashRefreshToken(refreshToken)).hasSize(64);
        assertThat(adapter.hashRefreshToken(refreshToken))
                .isEqualTo(adapter.hashRefreshToken(refreshToken));
    }
}
