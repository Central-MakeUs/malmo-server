package makeus.cmc.malmo.adaptor.out.persistence.repository.notification;

import makeus.cmc.malmo.adaptor.out.persistence.entity.notification.MemberNotificationEntity;
import makeus.cmc.malmo.domain.value.state.NotificationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MemberNotificationRepository extends JpaRepository<MemberNotificationEntity, Long> {

    @Query("select m from MemberNotificationEntity m where m.memberId.value = ?1 and m.state = ?2")
    List<MemberNotificationEntity> findByMemberIdAndState(Long memberId, NotificationState state);

    @Modifying
    @Transactional
    @Query("update MemberNotificationEntity m set m.state = 'READ' where m.id in ?1")
    void readNotifications(List<Long> notificationIds);

    @Query("select case when count(m) = ?3 then true else false end " +
            "from MemberNotificationEntity m " +
            "where m.id in ?1 and m.memberId.value = ?2")
    boolean areAllNotificationsBelongToMember(List<Long> notificationIds, Long memberId, long size);
}
