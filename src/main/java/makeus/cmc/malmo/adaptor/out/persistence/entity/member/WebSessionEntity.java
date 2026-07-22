package makeus.cmc.malmo.adaptor.out.persistence.entity.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "web_session",
        indexes = {
                @Index(name = "idx_web_session_member_id", columnList = "member_id"),
                @Index(name = "idx_web_session_expires_at", columnList = "expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    public static WebSessionEntity create(
            Long memberId,
            String refreshTokenHash,
            Instant expiresAt,
            Instant now
    ) {
        WebSessionEntity session = new WebSessionEntity();
        session.memberId = memberId;
        session.refreshTokenHash = refreshTokenHash;
        session.expiresAt = expiresAt;
        session.createdAt = now;
        session.modifiedAt = now;
        return session;
    }

    public boolean isActiveAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void rotate(String newRefreshTokenHash, Instant newExpiresAt, Instant now) {
        this.refreshTokenHash = newRefreshTokenHash;
        this.expiresAt = newExpiresAt;
        this.modifiedAt = now;
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
            modifiedAt = now;
        }
    }
}
