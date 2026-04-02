package makeus.cmc.malmo.integration_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import makeus.cmc.malmo.adaptor.in.web.controller.NotificationController;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.notification.MemberNotificationEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.application.port.out.member.GenerateTokenPort;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.state.NotificationState;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.NotificationType;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class MemberNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private GenerateTokenPort generateTokenPort;

    private String accessToken;
    private MemberEntity member;

    @BeforeEach
    void setup() {
        member = MemberEntity.builder()
                .provider(Provider.KAKAO)
                .providerId("testProviderId")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .email("testEmail@test.com")
                .nickname("nickname")
                .startLoveDate(LocalDate.of(2023, 10, 1))
                .inviteCodeEntityValue(InviteCodeEntityValue.of("testInviteCode"))
                .build();
        em.persist(member);
        em.flush();

        TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());
        accessToken = tokenInfo.getAccessToken();
    }

    @Nested
    @DisplayName("멤버 미조회 알림 조회")
    class GetPendingNotifications {

        @Test
        @DisplayName("알림이 없는 경우, 빈 리스트를 반환한다.")
        void getMemberPendingNotification_empty() throws Exception {
            // when & then
            mockMvc.perform(get("/members/notifications/pending")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notifications").doesNotExist());
        }

        @Test
        @DisplayName("알림이 있는 경우, 알림 목록을 반환한다.")
        void getMemberPendingNotification_success() throws Exception {
            // given
            createAndSaveNotification("테스트 알림 1");
            createAndSaveNotification("테스트 알림 2");

            // when & then
            mockMvc.perform(get("/members/notifications/pending")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.list.length()").value(2));
        }

        @Test
        @DisplayName("주간 리포트 발행 알림도 동일한 형식으로 조회된다.")
        void getMemberPendingNotification_includesWeeklyReportNotification() throws Exception {
            createAndSaveWeeklyNotification(
                    LocalDate.of(2026, 3, 23),
                    LocalDate.of(2026, 3, 29)
            );

            mockMvc.perform(get("/members/notifications/pending")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.list.length()").value(1))
                    .andExpect(jsonPath("$.data.list[0].type").value("WEEKLY_ANALYSIS_REPORT_PUBLISHED"))
                    .andExpect(jsonPath("$.data.list[0].payload.weekStartDate").value("2026-03-23"))
                    .andExpect(jsonPath("$.data.list[0].payload.weekEndDate").value("2026-03-29"));
        }
    }

    @Nested
    @DisplayName("멤버 미조회 알림 읽기 처리")
    class ProcessReadNotifications {

        @Test
        @DisplayName("알림이 있는 경우, 읽기 처리 성공")
        void processReadNotifications_success() throws Exception {
            // given
            MemberNotificationEntity notification1 = createAndSaveNotification("테스트 알림 1");
            MemberNotificationEntity notification2 = createAndSaveNotification("테스트 알림 2");

            NotificationController.ProcessNotificationsRequestDto requestDto = new NotificationController.ProcessNotificationsRequestDto();
            requestDto.setPendingNotifications(List.of(notification1.getId(), notification2.getId()));

            // when
            mockMvc.perform(patch("/members/notifications/pending")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            // then
            em.flush();
            em.clear();
            MemberNotificationEntity result1 = em.find(MemberNotificationEntity.class, notification1.getId());
            MemberNotificationEntity result2 = em.find(MemberNotificationEntity.class, notification2.getId());
            assertThat(result1.getState()).isEqualTo(NotificationState.READ);
            assertThat(result2.getState()).isEqualTo(NotificationState.READ);
        }

        @Test
        @DisplayName("주간 리포트 알림도 개별 읽기 처리할 수 있다.")
        void processReadNotifications_weeklyReportNotification_success() throws Exception {
            MemberNotificationEntity weeklyNotification = createAndSaveWeeklyNotification(
                    LocalDate.of(2026, 3, 23),
                    LocalDate.of(2026, 3, 29)
            );
            MemberNotificationEntity otherNotification = createAndSaveWeeklyNotification(
                    LocalDate.of(2026, 3, 30),
                    LocalDate.of(2026, 4, 5)
            );

            NotificationController.ProcessNotificationsRequestDto requestDto = new NotificationController.ProcessNotificationsRequestDto();
            requestDto.setPendingNotifications(List.of(weeklyNotification.getId()));

            mockMvc.perform(patch("/members/notifications/pending")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            em.flush();
            em.clear();

            MemberNotificationEntity updatedWeeklyNotification = em.find(MemberNotificationEntity.class, weeklyNotification.getId());
            MemberNotificationEntity untouchedWeeklyNotification = em.find(MemberNotificationEntity.class, otherNotification.getId());

            assertThat(updatedWeeklyNotification.getState()).isEqualTo(NotificationState.READ);
            assertThat(untouchedWeeklyNotification.getState()).isEqualTo(NotificationState.PENDING);
        }

        @Test
        @DisplayName("없는 알림 ID를 포함하여 요청하는 경우, 요청이 실패한다.")
        void processReadNotifications_with_not_exist_id() throws Exception {
            // given
            MemberNotificationEntity notification1 = createAndSaveNotification("테스트 알림 1");

            NotificationController.ProcessNotificationsRequestDto requestDto = new NotificationController.ProcessNotificationsRequestDto();
            requestDto.setPendingNotifications(List.of(notification1.getId(), 999L));

            // when
            mockMvc.perform(patch("/members/notifications/pending")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("알림이 자신의 것이 아닌 경우, 요청이 실패한다.")
        void processReadNotifications_not_mine() throws Exception {
            // given
            MemberEntity otherMember = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("otherProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .email("other@test.com")
                    .nickname("other")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("otherInviteCode"))
                    .build();
            em.persist(otherMember);
            em.flush();

            MemberNotificationEntity myNotification = createAndSaveNotification("내 알림");
            MemberNotificationEntity otherNotification = createAndSaveNotificationForMember("다른 사람 알림", otherMember);


            NotificationController.ProcessNotificationsRequestDto requestDto = new NotificationController.ProcessNotificationsRequestDto();
            requestDto.setPendingNotifications(List.of(myNotification.getId(), otherNotification.getId()));

            // when
            mockMvc.perform(patch("/members/notifications/pending")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());
        }
    }

    private MemberNotificationEntity createAndSaveNotification(String message) {
        return createAndSaveNotificationForMember(message, this.member);
    }

    private MemberNotificationEntity createAndSaveNotificationForMember(String message, MemberEntity member) {
        MemberNotificationEntity notification = MemberNotificationEntity.builder()
                .memberId(MemberEntityId.of(member.getId()))
                .payload(Map.of("message", message))
                .state(NotificationState.PENDING)
                .type(NotificationType.COUPLE_DISCONNECTED)
                .build();
        em.persist(notification);
        em.flush();
        return notification;
    }

    private MemberNotificationEntity createAndSaveWeeklyNotification(LocalDate weekStartDate, LocalDate weekEndDate) {
        MemberNotificationEntity notification = MemberNotificationEntity.builder()
                .memberId(MemberEntityId.of(member.getId()))
                .payload(Map.of(
                        "weekStartDate", weekStartDate.toString(),
                        "weekEndDate", weekEndDate.toString()
                ))
                .state(NotificationState.PENDING)
                .type(NotificationType.WEEKLY_ANALYSIS_REPORT_PUBLISHED)
                .build();
        em.persist(notification);
        em.flush();
        return notification;
    }
}
