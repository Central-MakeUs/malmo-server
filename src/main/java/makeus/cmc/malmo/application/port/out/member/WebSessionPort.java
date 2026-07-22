package makeus.cmc.malmo.application.port.out.member;

import java.time.Instant;

public interface WebSessionPort {

    void create(Long memberId, String refreshTokenHash, Instant expiresAt);

    Long rotate(String currentRefreshTokenHash, String newRefreshTokenHash, Instant newExpiresAt);

    void revoke(String refreshTokenHash);
}
