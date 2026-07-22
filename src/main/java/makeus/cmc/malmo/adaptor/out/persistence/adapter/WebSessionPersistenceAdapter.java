package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.WebSessionEntity;
import makeus.cmc.malmo.adaptor.out.persistence.repository.member.WebSessionRepository;
import makeus.cmc.malmo.application.exception.InvalidRefreshTokenException;
import makeus.cmc.malmo.application.port.out.member.WebSessionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class WebSessionPersistenceAdapter implements WebSessionPort {

    private final WebSessionRepository repository;
    private final Clock clock;

    @Override
    @Transactional
    public void create(Long memberId, String refreshTokenHash, Instant expiresAt) {
        repository.save(WebSessionEntity.create(
                memberId, refreshTokenHash, expiresAt, clock.instant()
        ));
    }

    @Override
    @Transactional
    public Long rotate(
            String currentRefreshTokenHash,
            String newRefreshTokenHash,
            Instant newExpiresAt
    ) {
        WebSessionEntity session = repository.findForUpdate(currentRefreshTokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();
        if (!session.isActiveAt(now)) {
            throw new InvalidRefreshTokenException();
        }
        session.rotate(newRefreshTokenHash, newExpiresAt, now);
        return session.getMemberId();
    }

    @Override
    @Transactional
    public void revoke(String refreshTokenHash) {
        repository.findForUpdate(refreshTokenHash)
                .ifPresent(session -> session.revoke(clock.instant()));
    }
}
