package makeus.cmc.malmo.application.helper.notification;

import makeus.cmc.malmo.application.port.out.notification.SaveNotificationPort;
import makeus.cmc.malmo.domain.model.notification.MemberNotification;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.NotificationState;
import makeus.cmc.malmo.domain.value.type.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberNotificationCommandHelperTest {

    @Mock
    private SaveNotificationPort saveNotificationPort;

    private MemberNotificationCommandHelper memberNotificationCommandHelper;

    @BeforeEach
    void setUp() {
        memberNotificationCommandHelper = new MemberNotificationCommandHelper(saveNotificationPort);
        when(saveNotificationPort.saveNotification(org.mockito.ArgumentMatchers.any(MemberNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createAndSaveWeeklyAnalysisReportPublishedNotification_createsOneNotificationPerWeek() {
        MemberId memberId = MemberId.of(1L);

        memberNotificationCommandHelper.createAndSaveWeeklyAnalysisReportPublishedNotification(
                memberId,
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 29)
        );
        memberNotificationCommandHelper.createAndSaveWeeklyAnalysisReportPublishedNotification(
                memberId,
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 4, 5)
        );

        ArgumentCaptor<MemberNotification> notificationCaptor = ArgumentCaptor.forClass(MemberNotification.class);
        verify(saveNotificationPort, times(2)).saveNotification(notificationCaptor.capture());

        assertThat(notificationCaptor.getAllValues())
                .extracting(MemberNotification::getType)
                .containsExactly(
                        NotificationType.WEEKLY_ANALYSIS_REPORT_PUBLISHED,
                        NotificationType.WEEKLY_ANALYSIS_REPORT_PUBLISHED
                );
        assertThat(notificationCaptor.getAllValues())
                .extracting(MemberNotification::getState)
                .containsExactly(NotificationState.PENDING, NotificationState.PENDING);
        assertThat(notificationCaptor.getAllValues())
                .extracting(notification -> notification.getPayload().get("weekStartDate"))
                .containsExactly("2026-03-23", "2026-03-30");
    }
}
