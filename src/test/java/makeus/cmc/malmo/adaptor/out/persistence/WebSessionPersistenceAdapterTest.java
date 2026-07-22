package makeus.cmc.malmo.adaptor.out.persistence;

import makeus.cmc.malmo.adaptor.out.persistence.adapter.WebSessionPersistenceAdapter;
import makeus.cmc.malmo.adaptor.out.persistence.repository.member.WebSessionRepository;
import makeus.cmc.malmo.application.exception.InvalidRefreshTokenException;
import makeus.cmc.malmo.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({WebSessionPersistenceAdapterTest.Config.class, QueryDslConfig.class})
class WebSessionPersistenceAdapterTest {

    @Autowired
    private WebSessionPersistenceAdapter adapter;

    @Autowired
    private WebSessionRepository repository;

    @Test
    void rotatesRefreshTokenAtomically() {
        Instant expiry = Instant.parse("2026-08-19T00:00:00Z");
        adapter.create(10L, "old-hash", expiry);

        Long memberId = adapter.rotate("old-hash", "new-hash", expiry.plusSeconds(60));

        assertThat(memberId).isEqualTo(10L);
        assertThat(repository.findByRefreshTokenHash("old-hash")).isEmpty();
        assertThat(repository.findByRefreshTokenHash("new-hash")).isPresent();
    }

    @Test
    void rejectsExpiredRefreshToken() {
        adapter.create(10L, "expired-hash", Instant.parse("2026-07-19T00:00:00Z"));

        assertThatThrownBy(() -> adapter.rotate(
                "expired-hash", "new-hash", Instant.parse("2026-08-19T00:00:00Z")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    static class Config {
        @org.springframework.context.annotation.Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC);
        }

        @org.springframework.context.annotation.Bean
        WebSessionPersistenceAdapter webSessionPersistenceAdapter(
                WebSessionRepository repository, Clock clock) {
            return new WebSessionPersistenceAdapter(repository, clock);
        }
    }
}
