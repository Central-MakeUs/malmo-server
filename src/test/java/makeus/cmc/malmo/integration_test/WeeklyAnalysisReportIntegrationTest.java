package makeus.cmc.malmo.integration_test;

import jakarta.persistence.EntityManager;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.DetailedPromptEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.weekly_analysis_report.WeeklyAnalysisReportEntity;
import makeus.cmc.malmo.application.helper.weekly_analysis_report.WeeklyAnalysisWeekCalculator;
import makeus.cmc.malmo.application.port.out.weekly_analysis_report.LoadWeeklyAnalysisReportPort;
import makeus.cmc.malmo.application.port.out.member.GenerateTokenPort;
import makeus.cmc.malmo.application.port.out.weekly_analysis_report.SaveWeeklyAnalysisReportPort;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReport;
import makeus.cmc.malmo.domain.model.weekly_analysis_report.WeeklyAnalysisReportContent;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.state.WeeklyAnalysisReportStatus;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class WeeklyAnalysisReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager em;

    @Autowired
    private GenerateTokenPort generateTokenPort;

    @Autowired
    private LoadWeeklyAnalysisReportPort loadWeeklyAnalysisReportPort;

    @Autowired
    private SaveWeeklyAnalysisReportPort saveWeeklyAnalysisReportPort;

    private String accessToken;
    private String adminAccessToken;
    private MemberEntity member;
    private MemberEntity adminMember;

    @BeforeEach
    void setUp() {
        member = MemberEntity.builder()
                .provider(Provider.KAKAO)
                .providerId("weekly-test-provider")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .email("weekly@test.com")
                .nickname("weekly")
                .inviteCodeEntityValue(InviteCodeEntityValue.of("weeklyInviteCode"))
                .build();
        em.persist(member);
        em.flush();

        TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());
        accessToken = tokenInfo.getAccessToken();

        adminMember = MemberEntity.builder()
                .provider(Provider.KAKAO)
                .providerId("weekly-admin-provider")
                .memberRole(MemberRole.ADMIN)
                .memberState(MemberState.ALIVE)
                .email("weekly-admin@test.com")
                .nickname("weekly-admin")
                .inviteCodeEntityValue(InviteCodeEntityValue.of("weeklyAdminCode"))
                .build();
        em.persist(adminMember);
        em.flush();

        TokenInfo adminTokenInfo = generateTokenPort.generateToken(adminMember.getId(), adminMember.getMemberRole());
        adminAccessToken = adminTokenInfo.getAccessToken();

        em.persist(DetailedPromptEntity.builder()
                .level(1)
                .detailedLevel(1)
                .content("test prompt")
                .isForValidation(false)
                .isForSummary(false)
                .metadataTitle("meta")
                .isLastDetailedPrompt(true)
                .isForGuideline(false)
                .build());
        em.flush();
    }

    @Test
    @DisplayName("발행된 주간 리포트 상세 조회에 성공한다")
    void getWeeklyReportDetail_success() throws Exception {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = new WeeklyAnalysisWeekCalculator().getPreviousClosedWeek();
        persistWeeklyReport(weekPeriod.getWeekStartDate(), weekPeriod.getWeekEndDate(), WeeklyAnalysisReportStatus.PUBLISHED);

        mockMvc.perform(get("/reports/weekly/{weekStartDate}", weekPeriod.getWeekStartDate())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekStartDate").value(weekPeriod.getWeekStartDate().toString()))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.schemaVersion").value("v1"))
                .andExpect(jsonPath("$.data.period.weekStartDate").value(weekPeriod.getWeekStartDate().toString()))
                .andExpect(jsonPath("$.data.overview.title").value("title"))
                .andExpect(jsonPath("$.data.topTopics[0].keyword").value("연락"))
                .andExpect(jsonPath("$.data.conflict.score").value(50))
                .andExpect(jsonPath("$.data.behaviorPattern.oneLineSummary").value("sum"))
                .andExpect(jsonPath("$.data.solution.content").value("content"));
    }

    @Test
    @DisplayName("생성 중인 주간 리포트 상세 조회는 404로 숨긴다")
    void getWeeklyReportDetail_hidesGenerating() throws Exception {
        WeeklyAnalysisWeekCalculator.WeekPeriod weekPeriod = new WeeklyAnalysisWeekCalculator().getPreviousClosedWeek();
        persistWeeklyReport(weekPeriod.getWeekStartDate(), weekPeriod.getWeekEndDate(), WeeklyAnalysisReportStatus.GENERATING);

        mockMvc.perform(get("/reports/weekly/{weekStartDate}", weekPeriod.getWeekStartDate())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("주간 리포트 목록 조회는 발행된 리포트만 주차 역순으로 반환한다")
    void getWeeklyReportList_success() throws Exception {
        persistWeeklyReport(LocalDate.of(2026, 3, 23), LocalDate.of(2026, 3, 29), WeeklyAnalysisReportStatus.PUBLISHED);
        persistWeeklyReport(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 5), WeeklyAnalysisReportStatus.PUBLISHED);
        persistWeeklyReport(LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 12), WeeklyAnalysisReportStatus.GENERATING);

        mockMvc.perform(get("/reports/weekly")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.list[0].weekStartDate").value("2026-03-30"))
                .andExpect(jsonPath("$.data.list[1].weekStartDate").value("2026-03-23"));
    }

    @Test
    @DisplayName("관리자는 특정 주차의 주간 리포트 수동 트리거를 실행할 수 있다")
    void triggerWeeklyReports_successForAdmin() throws Exception {
        mockMvc.perform(post("/admin/reports/weekly/{weekStartDate}/trigger", "2026-03-23")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekStartDate").value("2026-03-23"))
                .andExpect(jsonPath("$.data.weekEndDate").value("2026-03-29"))
                .andExpect(jsonPath("$.data.candidateMemberCount").exists())
                .andExpect(jsonPath("$.data.reservedCount").exists())
                .andExpect(jsonPath("$.data.republishedFailedCount").exists())
                .andExpect(jsonPath("$.data.skippedExistingCount").exists())
                .andExpect(jsonPath("$.data.skippedIneligibleCount").exists());
    }

    @Test
    @DisplayName("일반 회원은 주간 리포트 수동 트리거를 실행할 수 없다")
    void triggerWeeklyReports_forbiddenForMember() throws Exception {
        mockMvc.perform(post("/admin/reports/weekly/{weekStartDate}/trigger", "2026-03-23")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증이 없으면 주간 리포트 수동 트리거는 401을 반환한다")
    void triggerWeeklyReports_unauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/admin/reports/weekly/{weekStartDate}/trigger", "2026-03-23"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("월요일이 아닌 날짜로 수동 트리거하면 400을 반환한다")
    void triggerWeeklyReports_badRequestWhenNotMonday() throws Exception {
        mockMvc.perform(post("/admin/reports/weekly/{weekStartDate}/trigger", "2026-03-24")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("기존 리포트를 FAILED로 저장해도 createdAt이 유지된다")
    void saveExistingWeeklyReport_preservesCreatedAtOnFailureUpdate() {
        LocalDate weekStartDate = LocalDate.of(2026, 3, 23);
        LocalDate weekEndDate = LocalDate.of(2026, 3, 29);
        persistWeeklyReport(weekStartDate, weekEndDate, WeeklyAnalysisReportStatus.GENERATING);
        em.clear();

        WeeklyAnalysisReport report = loadWeeklyAnalysisReportPort.loadByMemberIdAndWeekStartDate(
                MemberId.of(member.getId()),
                weekStartDate
        ).orElseThrow();

        assertThat(report.getCreatedAt()).isNotNull();

        report.markFailed("llm timeout");
        WeeklyAnalysisReport savedReport = saveWeeklyAnalysisReportPort.saveAndFlushWeeklyAnalysisReport(report);
        em.clear();

        WeeklyAnalysisReportEntity persistedEntity = em.find(WeeklyAnalysisReportEntity.class, savedReport.getId());
        assertThat(savedReport.getCreatedAt()).isEqualTo(report.getCreatedAt());
        assertThat(savedReport.getStatus()).isEqualTo(WeeklyAnalysisReportStatus.FAILED);
        assertThat(persistedEntity.getCreatedAt()).isEqualTo(report.getCreatedAt());
        assertThat(persistedEntity.getStatus()).isEqualTo(WeeklyAnalysisReportStatus.FAILED);
        assertThat(persistedEntity.getFailedReason()).isEqualTo("llm timeout");
    }

    @Test
    @DisplayName("기존 리포트를 일반 save로 저장해도 createdAt이 유지된다")
    void saveExistingWeeklyReport_preservesCreatedAtOnFailureUpdateWithoutFlush() {
        LocalDate weekStartDate = LocalDate.of(2026, 3, 30);
        LocalDate weekEndDate = LocalDate.of(2026, 4, 5);
        persistWeeklyReport(weekStartDate, weekEndDate, WeeklyAnalysisReportStatus.GENERATING);
        em.clear();

        WeeklyAnalysisReport report = loadWeeklyAnalysisReportPort.loadByMemberIdAndWeekStartDate(
                MemberId.of(member.getId()),
                weekStartDate
        ).orElseThrow();

        assertThat(report.getCreatedAt()).isNotNull();

        report.markFailed("queue timeout");
        WeeklyAnalysisReport savedReport = saveWeeklyAnalysisReportPort.saveWeeklyAnalysisReport(report);
        em.flush();
        em.clear();

        WeeklyAnalysisReportEntity persistedEntity = em.find(WeeklyAnalysisReportEntity.class, savedReport.getId());
        assertThat(savedReport.getCreatedAt()).isEqualTo(report.getCreatedAt());
        assertThat(savedReport.getStatus()).isEqualTo(WeeklyAnalysisReportStatus.FAILED);
        assertThat(persistedEntity.getCreatedAt()).isEqualTo(report.getCreatedAt());
        assertThat(persistedEntity.getStatus()).isEqualTo(WeeklyAnalysisReportStatus.FAILED);
        assertThat(persistedEntity.getFailedReason()).isEqualTo("queue timeout");
    }

    private void persistWeeklyReport(LocalDate weekStartDate, LocalDate weekEndDate, WeeklyAnalysisReportStatus status) {
        WeeklyAnalysisReportContent content = sampleContent(weekStartDate, weekEndDate);

        WeeklyAnalysisReportEntity report = WeeklyAnalysisReportEntity.builder()
                .memberId(MemberEntityId.of(member.getId()))
                .weekStartDate(weekStartDate)
                .weekEndDate(weekEndDate)
                .status(status)
                .sourceChatRoomCount(1)
                .eligibleChatRoomCount(1)
                .sourceUserMessageCount(3)
                .schemaVersion(content.schemaVersion())
                .timezone(content.period().timezone())
                .overview(WeeklyAnalysisReportEntity.OverviewEmbeddable.from(content.overview()))
                .topTopics(new java.util.ArrayList<>(content.topTopics().stream()
                        .map(WeeklyAnalysisReportEntity.TopTopicEmbeddable::from)
                        .toList()))
                .moodByTime(WeeklyAnalysisReportEntity.MoodByTimeEmbeddable.from(content.moodByTime()))
                .conflict(WeeklyAnalysisReportEntity.ConflictEmbeddable.from(content.conflict()))
                .behaviorPattern(WeeklyAnalysisReportEntity.BehaviorPatternEmbeddable.from(content.behaviorPattern()))
                .solution(WeeklyAnalysisReportEntity.SolutionEmbeddable.from(content.solution()))
                .generatedAt(java.time.LocalDateTime.now())
                .build();
        em.persist(report);
        em.flush();
    }

    private WeeklyAnalysisReportContent sampleContent(LocalDate weekStartDate, LocalDate weekEndDate) {
        return new WeeklyAnalysisReportContent(
                "v1",
                new WeeklyAnalysisReportContent.Period(weekStartDate, weekEndDate, "Asia/Seoul"),
                new WeeklyAnalysisReportContent.Overview("title", "summary"),
                java.util.List.of(new WeeklyAnalysisReportContent.TopTopic("연락", 1, 0.5, "desc")),
                new WeeklyAnalysisReportContent.MoodByTime(
                        "EVENING",
                        new WeeklyAnalysisReportContent.MoodRatios(0.1, 0.2, 0.6, 0.1),
                        "desc"
                ),
                new WeeklyAnalysisReportContent.Conflict(50, "desc"),
                new WeeklyAnalysisReportContent.BehaviorPattern("sum", "trigger", "belief", "response"),
                new WeeklyAnalysisReportContent.Solution("title", "content")
        );
    }
}
