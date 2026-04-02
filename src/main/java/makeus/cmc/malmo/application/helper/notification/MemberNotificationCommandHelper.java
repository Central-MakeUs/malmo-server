package makeus.cmc.malmo.application.helper.notification;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.notification.SaveNotificationPort;
import makeus.cmc.malmo.domain.model.notification.MemberNotification;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.NotificationState;
import makeus.cmc.malmo.domain.value.type.NotificationType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class MemberNotificationCommandHelper {

    private final SaveNotificationPort saveNotificationPort;

    public MemberNotification createAndSaveCoupleDisconnectedNotification(MemberId memberId) {
        return createAndSavePendingNotification(memberId, NotificationType.COUPLE_DISCONNECTED, null);
    }

    private MemberNotification createAndSavePendingNotification(MemberId memberId, NotificationType type, Map<String, Object> payload) {
        MemberNotification memberNotification = MemberNotification.createMemberNotification(
                memberId,
                type,
                NotificationState.PENDING,
                payload
        );

        return saveNotificationPort.saveNotification(memberNotification);
    }

    public void markNotificationsAsRead(List<Long> notificationIds) {
        saveNotificationPort.readNotifications(notificationIds);
    }

    public MemberNotification createAndSaveCoupleConnectedNotification(MemberId memberId) {
        return createAndSavePendingNotification(memberId, NotificationType.COUPLE_CONNECTED, null);
    }

    public MemberNotification createAndSaveWeeklyAnalysisReportPublishedNotification(
            MemberId memberId,
            LocalDate weekStartDate,
            LocalDate weekEndDate
    ) {
        return createAndSavePendingNotification(
                memberId,
                NotificationType.WEEKLY_ANALYSIS_REPORT_PUBLISHED,
                Map.of(
                        "weekStartDate", weekStartDate.toString(),
                        "weekEndDate", weekEndDate.toString()
                )
        );
    }
}
