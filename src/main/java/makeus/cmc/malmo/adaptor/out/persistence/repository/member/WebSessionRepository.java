package makeus.cmc.malmo.adaptor.out.persistence.repository.member;

import jakarta.persistence.LockModeType;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.WebSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WebSessionRepository extends JpaRepository<WebSessionEntity, Long> {

    Optional<WebSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from WebSessionEntity s where s.refreshTokenHash = :refreshTokenHash")
    Optional<WebSessionEntity> findForUpdate(@Param("refreshTokenHash") String refreshTokenHash);
}
