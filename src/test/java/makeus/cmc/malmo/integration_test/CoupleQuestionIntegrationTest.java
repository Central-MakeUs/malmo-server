package makeus.cmc.malmo.integration_test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import makeus.cmc.malmo.adaptor.in.web.controller.QuestionController;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.question.CoupleQuestionEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.question.MemberAnswerEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.question.QuestionEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.question.TempCoupleQuestionEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.CoupleQuestionEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.application.helper.outbox.OutboxHelper;
import makeus.cmc.malmo.application.port.out.member.GenerateTokenPort;
import makeus.cmc.malmo.domain.value.state.CoupleQuestionState;
import makeus.cmc.malmo.domain.value.state.MemberAnswerState;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.integration_test.dto_factory.CoupleQuestionRequestDtoFactory;
import makeus.cmc.malmo.integration_test.dto_factory.CoupleRequestDtoFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static makeus.cmc.malmo.adaptor.in.exception.ErrorCode.*;
import static makeus.cmc.malmo.util.GlobalConstants.FIRST_QUESTION_LEVEL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class CoupleQuestionIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    OutboxHelper outboxHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private GenerateTokenPort generateTokenPort;

    private String accessToken;

    private String partnerAccessToken;

    private MemberEntity member;

    private MemberEntity partner;

    private MemberEntity other;

    @BeforeEach
    void setup() {
        member = MemberEntity.builder()
                .provider(Provider.KAKAO)
                .providerId("testProviderId")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .email("testEmail@test.com")
                .inviteCodeEntityValue(InviteCodeEntityValue.of("invite1"))
                .build();

        partner = MemberEntity.builder()
                .provider(Provider.KAKAO)
                .providerId("partnerProviderId")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .email("testEmail2@test.com")
                .inviteCodeEntityValue(InviteCodeEntityValue.of("invite2"))
                .build();

        other = MemberEntity.builder()
                .provider(Provider.KAKAO)
                .providerId("otherProviderId")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .email("testEmail3@test.com")
                .inviteCodeEntityValue(InviteCodeEntityValue.of("invite3"))
                .build();

        em.persist(member);
        em.persist(partner);
        em.persist(other);
        em.flush();

        TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());
        TokenInfo partnerTokenInfo = generateTokenPort.generateToken(partner.getId(), partner.getMemberRole());

        accessToken = tokenInfo.getAccessToken();
        partnerAccessToken = partnerTokenInfo.getAccessToken();
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class ResponseDto <T> {
        String requestId;
        boolean success;
        String message;
        T data;
    }

    @Nested
    @DisplayName("오늘의 질문 조회 기능 검증")
    class CoupleTodayQuestionFeature {

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public static class GetQuestionResponse {
            Long coupleQuestionId;
            String title;
            String content;
            int level;
            boolean meAnswered;
            boolean partnerAnswered;
            LocalDateTime createdAt;
        }

        @Test
        @DisplayName("커플이 아닌 멤버 오늘의 질문 조회 성공")
        void 커플_x_멤버_오늘의_질문_조회_성공() throws Exception {
            // when
            MvcResult mvcResult = mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(new MediaType("application", "json", StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<GetQuestionResponse> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            GetQuestionResponse data = responseDto.data;
            QuestionEntity question = em.createQuery("SELECT q FROM QuestionEntity q WHERE q.level = :level", QuestionEntity.class)
                    .setParameter("level", FIRST_QUESTION_LEVEL)
                    .getSingleResult();

            TempCoupleQuestionEntity tempQuestion = em.createQuery("SELECT tq FROM TempCoupleQuestionEntity tq WHERE tq.memberId.value = :memberId", TempCoupleQuestionEntity.class)
                    .setParameter("memberId", member.getId())
                    .getSingleResult();

            Assertions.assertThat(data.content).isEqualTo(question.getContent());
            Assertions.assertThat(data.title).isEqualTo(question.getTitle());
            Assertions.assertThat(data.level).isEqualTo(FIRST_QUESTION_LEVEL);
            Assertions.assertThat(data.meAnswered).isFalse();
            Assertions.assertThat(data.partnerAnswered).isFalse();
            Assertions.assertThat(data.createdAt).isNotNull();

            Assertions.assertThat(tempQuestion.getAnswer()).isNull();
            Assertions.assertThat(tempQuestion.getQuestion().getId()).isEqualTo(question.getId());
            Assertions.assertThat(tempQuestion.getCoupleQuestionState()).isEqualTo(CoupleQuestionState.ALIVE);
        }

        @Test
        @DisplayName("커플인 멤버 오늘의 질문 조회 성공")
        void 커플_멤버_오늘의_질문_조회_성공() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when
            MvcResult mvcResult = mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(new MediaType("application", "json", StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<GetQuestionResponse> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            GetQuestionResponse data = responseDto.data;
            QuestionEntity question = em.createQuery("SELECT q FROM QuestionEntity q WHERE q.level = :level", QuestionEntity.class)
                    .setParameter("level", FIRST_QUESTION_LEVEL)
                    .getSingleResult();
            Assertions.assertThat(data.content).isEqualTo(question.getContent());
            Assertions.assertThat(data.title).isEqualTo(question.getTitle());
            Assertions.assertThat(data.level).isEqualTo(FIRST_QUESTION_LEVEL);
            Assertions.assertThat(data.meAnswered).isFalse();
            Assertions.assertThat(data.partnerAnswered).isFalse();
            Assertions.assertThat(data.createdAt).isNotNull();
        }

        @Test
        @DisplayName("커플인 멤버 오늘의 질문 조회 성공 - 내 답변이 작성된 경우")
        void 커플_멤버_오늘의_질문_조회_성공_내_답변_작성() throws Exception {
            // given
            QuestionEntity question = em.createQuery("SELECT q FROM QuestionEntity q WHERE q.level = :level", QuestionEntity.class)
                    .setParameter("level", FIRST_QUESTION_LEVEL)
                    .getSingleResult();

            TempCoupleQuestionEntity tempQuestion = TempCoupleQuestionEntity.builder()
                    .question(question)
                    .memberId(MemberEntityId.of(member.getId()))
                    .answer("내 답변")
                    .coupleQuestionState(CoupleQuestionState.ALIVE)
                    .build();

            em.persist(tempQuestion);
            em.flush();

            // when
            MvcResult mvcResult = mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(new MediaType("application", "json", StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<GetQuestionResponse> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            GetQuestionResponse data = responseDto.data;

            Assertions.assertThat(data.content).isEqualTo(question.getContent());
            Assertions.assertThat(data.title).isEqualTo(question.getTitle());
            Assertions.assertThat(data.level).isEqualTo(FIRST_QUESTION_LEVEL);
            Assertions.assertThat(data.meAnswered).isTrue();
            Assertions.assertThat(data.partnerAnswered).isFalse();
            Assertions.assertThat(data.createdAt).isNotNull();
        }

        @Test
        @DisplayName("커플인 멤버 오늘의 질문 조회 성공 - 상대방 답변이 작성된 경우")
        void 커플_멤버_오늘의_질문_조회_성공_상대방_답변_작성() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();

            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            MemberAnswerEntity answer = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(partner.getId()))
                    .answer("상대방 답변")
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();

            em.persist(answer);
            em.flush();

            // when
            MvcResult mvcResult = mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(new MediaType("application", "json", StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<GetQuestionResponse> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            GetQuestionResponse data = responseDto.data;
            QuestionEntity question = em.createQuery("SELECT q FROM QuestionEntity q WHERE q.level = :level", QuestionEntity.class)
                    .setParameter("level", FIRST_QUESTION_LEVEL)
                    .getSingleResult();
            Assertions.assertThat(data.content).isEqualTo(question.getContent());
            Assertions.assertThat(data.title).isEqualTo(question.getTitle());
            Assertions.assertThat(data.level).isEqualTo(FIRST_QUESTION_LEVEL);
            Assertions.assertThat(data.meAnswered).isFalse();
            Assertions.assertThat(data.partnerAnswered).isTrue();
            Assertions.assertThat(data.createdAt).isNotNull();
        }

        @Test
        @DisplayName("커플인 멤버 오늘의 질문 조회 성공 - 전날 완성된 오늘의 질문 조회")
        void 커플_멤버_오늘의_질문_조회_성공_전날_완성된_오늘의_질문() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();

            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            em.createQuery("UPDATE CoupleQuestionEntity cq SET cq.coupleQuestionState = :state, cq.bothAnsweredAt = :bothAnsweredAt WHERE cq.id = :id")
                    .setParameter("state", CoupleQuestionState.COMPLETED)
                    .setParameter("bothAnsweredAt", LocalDateTime.now().minusDays(1))
                    .setParameter("id", coupleQuestion.getId())
                    .executeUpdate();

            doNothing().when(outboxHelper).publish(any(), any());

            // when
            MvcResult mvcResult = mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(new MediaType("application", "json", StandardCharsets.UTF_8)))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<GetQuestionResponse> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            // 이전 질문이 완료된 상태이므로, 오늘의 질문은 다음 레벨의 질문이 조회되어야 함
            GetQuestionResponse data = responseDto.data;
            QuestionEntity question = em.createQuery("SELECT q FROM QuestionEntity q WHERE q.level = :level", QuestionEntity.class)
                    .setParameter("level", FIRST_QUESTION_LEVEL + 1)
                    .getSingleResult();
            Assertions.assertThat(data.content).isEqualTo(question.getContent());
            Assertions.assertThat(data.title).isEqualTo(question.getTitle());
            Assertions.assertThat(data.level).isEqualTo(FIRST_QUESTION_LEVEL + 1);
            Assertions.assertThat(data.meAnswered).isFalse();
            Assertions.assertThat(data.partnerAnswered).isFalse();

            // publishStreamMessagePort 호출 검증
            verify(outboxHelper, times(2)).publish(any(), any());
        }
    }

    @Nested
    @DisplayName("과거 질문 조회 기능 검증")
    class CoupleBeforeQuestionFeature {

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public static class GetQuestionResponse {
            Long coupleQuestionId;
            String title;
            String content;
            int level;
            boolean meAnswered;
            boolean partnerAnswered;
            LocalDateTime createdAt;
        }

        @Test
        @DisplayName("커플인 멤버가 과거 질문 조회 성공")
        void 커플_멤버_과거_질문_조회_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();
            em.createQuery("UPDATE CoupleQuestionEntity cq SET cq.coupleQuestionState = :state, cq.bothAnsweredAt = :bothAnsweredAt WHERE cq.id = :id")
                    .setParameter("state", CoupleQuestionState.OUTDATED)
                    .setParameter("bothAnsweredAt", LocalDateTime.now().minusDays(3))
                    .setParameter("id", coupleQuestion.getId())
                    .executeUpdate();

            QuestionEntity question1 = em.createQuery("SELECT q FROM QuestionEntity q WHERE q.level = :level", QuestionEntity.class)
                    .setParameter("level", FIRST_QUESTION_LEVEL + 1)
                    .getSingleResult();

            QuestionEntity question2 = em.createQuery("SELECT q FROM QuestionEntity q WHERE q.level = :level", QuestionEntity.class)
                    .setParameter("level", FIRST_QUESTION_LEVEL + 2)
                    .getSingleResult();

            CoupleQuestionEntity coupleQuestion1 = CoupleQuestionEntity.builder()
                    .coupleEntityId(coupleQuestion.getCoupleEntityId())
                    .question(question1)
                    .coupleQuestionState(CoupleQuestionState.OUTDATED)
                    .bothAnsweredAt(LocalDateTime.now().minusDays(2))
                    .build();

            CoupleQuestionEntity coupleQuestion2 = CoupleQuestionEntity.builder()
                    .coupleEntityId(coupleQuestion.getCoupleEntityId())
                    .question(question2)
                    .coupleQuestionState(CoupleQuestionState.OUTDATED)
                    .bothAnsweredAt(LocalDateTime.now().minusDays(2))
                    .build();

            em.persist(coupleQuestion1);
            em.persist(coupleQuestion2);

            MemberAnswerEntity answer = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion1.getId()))
                    .memberEntityId(MemberEntityId.of(partner.getId()))
                    .answer("상대방 답변")
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();

            em.persist(answer);
            em.flush();
            // when
            MvcResult mvcResult = mockMvc.perform(get("/questions/" + question1.getLevel())
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<GetQuestionResponse> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            GetQuestionResponse data = responseDto.data;
            Assertions.assertThat(data.content).isEqualTo(question1.getContent());
            Assertions.assertThat(data.title).isEqualTo(question1.getTitle());
            Assertions.assertThat(data.level).isEqualTo(FIRST_QUESTION_LEVEL + 1);
            Assertions.assertThat(data.meAnswered).isFalse();
            Assertions.assertThat(data.partnerAnswered).isTrue();
            Assertions.assertThat(data.createdAt).isNotNull();
        }

        @Test
        @DisplayName("커플이 아닌 멤버 과거 질문 조회 실패")
        void 커플_x_멤버_과거_질문_조회_실패() throws Exception {
            // when & then
            mockMvc.perform(get("/questions/" + 3)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("message").value(NOT_COUPLE_MEMBER.getMessage()))
                    .andExpect(jsonPath("code").value(NOT_COUPLE_MEMBER.getCode()));
        }

        @Test
        @DisplayName("커플인 멤버가 현재 단계 이후의 질문 조회 실패")
        void 커플_멤버_현재_단계_이후의_질문_조회_실패() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/questions/" + 3)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_COUPLE_QUESTION.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_COUPLE_QUESTION.getCode()));
        }
    }

    @Nested
    @DisplayName("질문 답변 기능 검증")
    class CoupleAnswerQuestionFeature {
        @Test
        @DisplayName("커플이 아닌 멤버 오늘의 질문 답변 등록 성공")
        void 커플_아닌_멤버_오늘의_질문_답변_등록_성공() throws Exception {
            // given
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            String answer = "my answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            // when
            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            // then
            TempCoupleQuestionEntity tempCoupleQuestion = em.createQuery("select t from TempCoupleQuestionEntity t where t.memberId.value = :memberId", TempCoupleQuestionEntity.class)
                    .setParameter("memberId", member.getId())
                    .getSingleResult();
            Assertions.assertThat(tempCoupleQuestion.getAnswer()).isEqualTo(answer);
        }

        @Test
        @DisplayName("커플인 멤버 오늘의 질문 답변 등록 성공")
        void 커플인_멤버_오늘의_질문_답변_등록_성공() throws Exception {
            // given
            MvcResult mvcResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk()).andReturn();

            String answer = "my answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            // when
            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            // then
            MemberAnswerEntity memberAnswer = em.createQuery("select ma from MemberAnswerEntity ma where ma.memberEntityId.value = :memberId", MemberAnswerEntity.class)
                    .setParameter("memberId", member.getId())
                    .getSingleResult();
            Assertions.assertThat(memberAnswer.getAnswer()).isEqualTo(answer);
        }

        @Test
        @DisplayName("커플이 아닌 멤버 이미 답변을 단 상태에서 답변 등록 실패")
        void 커플_아닌_멤버_이미_답변_등록_실패() throws Exception {
            // given
            QuestionEntity question = em.createQuery("SELECT q FROM QuestionEntity q WHERE q.level = :level", QuestionEntity.class)
                    .setParameter("level", FIRST_QUESTION_LEVEL)
                    .getSingleResult();

            TempCoupleQuestionEntity tempQuestion = TempCoupleQuestionEntity.builder()
                    .question(question)
                    .memberId(MemberEntityId.of(member.getId()))
                    .answer("내 답변")
                    .coupleQuestionState(CoupleQuestionState.ALIVE)
                    .build();

            em.persist(tempQuestion);
            em.flush();

            String answer = "new answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            // when & then
            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }

        @Test
        @DisplayName("커플인 멤버 이미 답변을 단 상태에서 답변 등록 실패")
        void 커플인_멤버_이미_답변_등록_실패() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            MemberAnswerEntity answerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(member.getId()))
                    .answer("my answer")
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(answerEntity);
            em.flush();

            String answer = "new answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            // when & then
            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }

        @Test
        @DisplayName("커플인 멤버 답변을 모두 단 후 24시간 이후 새로운 질문 생성 전 답변 등록 실패")
        void 커플인_멤버_답변_모두_단_후_24시간_이내_답변_등록_실패() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            em.createQuery("UPDATE CoupleQuestionEntity cq SET cq.coupleQuestionState = :state, cq.bothAnsweredAt = :bothAnsweredAt WHERE cq.id = :id")
                    .setParameter("state", CoupleQuestionState.COMPLETED)
                    .setParameter("bothAnsweredAt", LocalDateTime.now().minusHours(1))
                    .setParameter("id", coupleQuestion.getId())
                    .executeUpdate();

            MemberAnswerEntity answerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(member.getId()))
                    .answer("my answer")
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(answerEntity);
            em.flush();

            String answer = "new answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            // when & then
            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }
    }

    @Nested
    @DisplayName("질문 답변 조회 기능 검증")
    class GetCoupleAnswerQuestionFeature {
        @Test
        @DisplayName("커플이 아닌 멤버 답변이 없는 경우 답변 조회 성공")
        void 커플_아닌_멤버_답변_없는_경우_답변_조회_성공() throws Exception {
            // given
            mockMvc.perform(get("/questions/today")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            TempCoupleQuestionEntity tempCoupleQuestion = em.createQuery("select t from TempCoupleQuestionEntity t where t.memberId.value = :memberId", TempCoupleQuestionEntity.class)
                    .setParameter("memberId", member.getId())
                    .getSingleResult();

            // when & then
            mockMvc.perform(get("/questions/" + tempCoupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.me.answer").isEmpty())
                    .andExpect(jsonPath("$.data.partner").isEmpty());
        }

        @Test
        @DisplayName("커플이 아닌 멤버 오늘의 질문 답변 조회 성공")
        void 커플_아닌_멤버_오늘의_질문_답변_조회_성공() throws Exception {
            // given
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            TempCoupleQuestionEntity tempCoupleQuestion = em.createQuery("select t from TempCoupleQuestionEntity t where t.memberId.value = :memberId", TempCoupleQuestionEntity.class)
                    .setParameter("memberId", member.getId())
                    .getSingleResult();

            String answer = "my answer";
            em.createQuery("UPDATE TempCoupleQuestionEntity t SET t.answer = :answer WHERE t.id = :id")
                    .setParameter("answer", answer)
                    .setParameter("id", tempCoupleQuestion.getId())
                    .executeUpdate();

            em.clear();

            // when & then
            mockMvc.perform(get("/questions/" + tempCoupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.me.answer").value(answer))
                    .andExpect(jsonPath("$.data.me.nickname").value(member.getNickname()))
                    .andExpect(jsonPath("$.data.partner").isEmpty());
        }

        @Test
        @DisplayName("커플인 멤버 답변이 없는 경우 답변 조회 성공")
        void 커플인_멤버_답변_없는_경우_답변_조회_성공() throws Exception {
            // given
            MvcResult mvcResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();

            String coupleContent = mvcResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("select cq from CoupleQuestionEntity cq where cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            // when & then
            mockMvc.perform(get("/questions/" + coupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.me.answer").isEmpty())
                    .andExpect(jsonPath("$.data.me.nickname").value(member.getNickname()))
                    .andExpect(jsonPath("$.data.partner.answer").isEmpty())
                    .andExpect(jsonPath("$.data.partner.nickname").value(partner.getNickname()));
        }

        @Test
        @DisplayName("커플인 멤버 자신만 답변을 단 경우 오늘의 질문 답변 조회 성공")
        void 커플인_멤버_자신만_답변_단_경우_오늘의_질문_답변_조회_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            String answer = "my answer";
            MemberAnswerEntity answerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(member.getId()))
                    .answer(answer)
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(answerEntity);
            em.flush();

            // when & then
            mockMvc.perform(get("/questions/" + coupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.me.answer").value(answer))
                    .andExpect(jsonPath("$.data.partner.answer").isEmpty());
        }

        @Test
        @DisplayName("커플인 멤버 상대방만 답변을 단 경우 오늘의 질문 답변 조회 성공")
        void 커플인_멤버_상대방만_답변_단_경우_오늘의_질문_답변_조회_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            String partnerAnswer = "partner's answer";
            MemberAnswerEntity answerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(partner.getId()))
                    .answer(partnerAnswer)
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(answerEntity);
            em.flush();

            // when & then
            mockMvc.perform(get("/questions/" + coupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.me.answer").isEmpty())
                    .andExpect(jsonPath("$.data.partner.answer").value(partnerAnswer));
        }

        @Test
        @DisplayName("커플인 멤버 답변을 모두 단 경우 오늘의 질문 답변 조회 성공")
        void 커플인_멤버_답변_모두_단_경우_오늘의_질문_답변_조회_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            String myAnswer = "my answer";
            MemberAnswerEntity myAnswerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(member.getId()))
                    .answer(myAnswer)
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(myAnswerEntity);

            String partnerAnswer = "partner's answer";
            MemberAnswerEntity partnerAnswerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(partner.getId()))
                    .answer(partnerAnswer)
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(partnerAnswerEntity);
            em.flush();

            // when & then
            mockMvc.perform(get("/questions/" + coupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.me.answer").value(myAnswer))
                    .andExpect(jsonPath("$.data.partner.answer").value(partnerAnswer));
        }

        @Test
        @DisplayName("커플인 멤버 과거 질문 답변 조회 성공")
        void 커플인_멤버_과거_질문_답변_조회_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            em.createQuery("UPDATE CoupleQuestionEntity cq SET cq.coupleQuestionState = :state, cq.bothAnsweredAt = :bothAnsweredAt WHERE cq.id = :id")
                    .setParameter("state", CoupleQuestionState.OUTDATED)
                    .setParameter("bothAnsweredAt", LocalDateTime.now().minusDays(1))
                    .setParameter("id", coupleQuestion.getId())
                    .executeUpdate();

            String myAnswer = "my answer";
            MemberAnswerEntity myAnswerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(member.getId()))
                    .answer(myAnswer)
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(myAnswerEntity);

            String partnerAnswer = "partner's answer";
            MemberAnswerEntity partnerAnswerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(partner.getId()))
                    .answer(partnerAnswer)
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(partnerAnswerEntity);
            em.flush();

            // when & then
            mockMvc.perform(get("/questions/" + coupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.me.answer").value(myAnswer))
                    .andExpect(jsonPath("$.data.me.updatable").value(false))
                    .andExpect(jsonPath("$.data.partner.answer").value(partnerAnswer))
                    .andExpect(jsonPath("$.data.partner.updatable").value(false));
        }

        @Test
        @DisplayName("커플인 멤버 다른 커플 소유의 질문 답변 조회 실패")
        void 커플인_멤버_다른_커플_소유의_질문_답변_조회_실패() throws Exception {
            // given
            // partner - other 연결
            MvcResult mvcResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + partnerAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(other.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();

            // me - partner 연결
            MemberEntity partner2 = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("secondPartnerProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .email("testEmail3@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("invite4"))
                    .build();

            em.persist(partner2);
            em.flush();

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner2.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // partner - other 커플 질문 조회
            String coupleContent = mvcResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            // when & then
            // me가 다른 커플의 질문 답변 조회 시도
            mockMvc.perform(get("/questions/" + coupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }
    }

    @Nested
    @DisplayName("질문 답변 수정 기능 검증")
    class UpdateCoupleAnswerQuestionFeature {
        @Test
        @DisplayName("커플이 아닌 멤버 오늘의 질문 답변 수정 성공")
        void 커플_아닌_멤버_오늘의_질문_답변_수정_성공() throws Exception {
            // given
            QuestionEntity question = em.createQuery("SELECT q FROM QuestionEntity q WHERE q.level = :level", QuestionEntity.class)
                    .setParameter("level", FIRST_QUESTION_LEVEL)
                    .getSingleResult();

            TempCoupleQuestionEntity tempQuestion = TempCoupleQuestionEntity.builder()
                    .question(question)
                    .memberId(MemberEntityId.of(member.getId()))
                    .answer("내 답변")
                    .coupleQuestionState(CoupleQuestionState.ALIVE)
                    .build();

            em.persist(tempQuestion);
            em.flush();

            String newAnswer = "new answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(newAnswer);

            // when
            mockMvc.perform(patch("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            // then
            TempCoupleQuestionEntity tempCoupleQuestion = em.createQuery("select t from TempCoupleQuestionEntity t where t.memberId.value = :memberId", TempCoupleQuestionEntity.class)
                    .setParameter("memberId", member.getId())
                    .getSingleResult();
            Assertions.assertThat(tempCoupleQuestion.getAnswer()).isEqualTo(newAnswer);
        }

        @Test
        @DisplayName("커플인 멤버 오늘의 질문 답변 수정 성공")
        void 커플인_멤버_오늘의_질문_답변_수정_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            MemberAnswerEntity answerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(member.getId()))
                    .answer("my answer")
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(answerEntity);
            em.flush();

            String newAnswer = "new answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(newAnswer);

            // when
            mockMvc.perform(patch("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            // then
            MemberAnswerEntity memberAnswer = em.createQuery("select ma from MemberAnswerEntity ma where ma.memberEntityId.value = :memberId", MemberAnswerEntity.class)
                    .setParameter("memberId", member.getId())
                    .getSingleResult();
            Assertions.assertThat(memberAnswer.getAnswer()).isEqualTo(newAnswer);
        }

        @Test
        @DisplayName("커플이 아닌 멤버 답변을 달지 않은 상태에서 답변 수정 실패")
        void 커플_아닌_멤버_답변_달지_않은_상태에서_답변_수정_실패() throws Exception {
            // given
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            String newAnswer = "new answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(newAnswer);

            // when & then
            mockMvc.perform(patch("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }

        @Test
        @DisplayName("커플인 멤버 답변을 달지 않은 상태에서 답변 수정 실패")
        void 커플인_멤버_답변_달지_않은_상태에서_답변_수정_실패() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            String newAnswer = "new answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(newAnswer);

            // when & then
            mockMvc.perform(patch("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }

        @Test
        @DisplayName("커플인 멤버 답변을 모두 단 후 24시간 이후 새로운 질문 생성 전 답변 수정 실패")
        void 커플인_멤버_답변_모두_단_후_24시간_이후_답변_수정_실패() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            em.createQuery("UPDATE CoupleQuestionEntity cq SET cq.coupleQuestionState = :state, cq.bothAnsweredAt = :bothAnsweredAt WHERE cq.id = :id")
                    .setParameter("state", CoupleQuestionState.COMPLETED)
                    .setParameter("bothAnsweredAt", LocalDateTime.now().minusHours(25))
                    .setParameter("id", coupleQuestion.getId())
                    .executeUpdate();

            String newAnswer = "new answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(newAnswer);

            // when & then
            mockMvc.perform(patch("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }
    }


    @Nested
    @DisplayName("다양한 커플 연동 케이스 검증")
    class VariousCaseQuestionFeature {
        @Test
        @DisplayName("커플 연결이 끊어진 경우 오늘의 질문 조회 성공")
        void 커플_연결_끊어진_경우_오늘의_질문_조회_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            MemberAnswerEntity answerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(member.getId()))
                    .answer("my answer")
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();
            em.persist(answerEntity);
            em.flush();

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.meAnswered").value(false))
                    .andExpect(jsonPath("$.data.partnerAnswered").value(false));
        }

        @Test
        @DisplayName("커플 연결이 끊어진 경우 오늘의 질문 답변 등록 성공")
        void 커플_연결_끊어진_경우_오늘의_질문_답변_등록_성공() throws Exception {
            // given
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            String answer = "my answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            // when & then
            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("커플 연결이 끊어진 경우 이전 임시 질문 답변 수정 실패")
        void 커플_연결_끊어진_경우_오늘의_질문_답변_수정_실패() throws Exception {
            // given
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            String answer = "my answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            String newAnswer = "new answer";
            QuestionController.AnswerRequestDto newRequestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(newAnswer);

            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(patch("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newRequestDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }

        @Test
        @DisplayName("커플 연결이 끊어진 경우 과거 질문 조회 실패")
        void 커플_연결_끊어진_경우_과거_질문_조회_실패() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/questions/1")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(NOT_COUPLE_MEMBER.getMessage()))
                    .andExpect(jsonPath("$.code").value(NOT_COUPLE_MEMBER.getCode()));
        }

        @Test
        @DisplayName("커플 연결이 끊어진 경우 과거 질문이 아닌 임시 답변 조회 성공")
        void 커플_연결_끊어진_경우_과거_질문_답변_조회_성공() throws Exception {
            // given
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            MemberAnswerEntity answerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(partner.getId()))
                    .answer("my answer")
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();

            em.persist(answerEntity);
            em.flush();

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // when & then
            // 퍼트너가 답변을 단 과거 질문이 아닌 임시 질문으로 조회
            mockMvc.perform(get("/questions/" + coupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.partner").isEmpty());
        }

        @Test
        @DisplayName("커플 연결이 끊어진 이후 재결합한 경우 오늘의 질문 조회 성공")
        void 커플_재결합_후_오늘의_질문_조회_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            MemberAnswerEntity answerEntity = MemberAnswerEntity.builder()
                    .coupleQuestionEntityId(CoupleQuestionEntityId.of(coupleQuestion.getId()))
                    .memberEntityId(MemberEntityId.of(partner.getId()))
                    .answer("my answer")
                    .memberAnswerState(MemberAnswerState.ALIVE)
                    .build();

            em.persist(answerEntity);
            em.flush();

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when & then
            // 커플 끊기 전 답변도 확인 가능해야 함
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.meAnswered").value(false))
                    .andExpect(jsonPath("$.data.partnerAnswered").value(true));
        }

        @Test
        @DisplayName("커플 연결이 끊어진 이후 재결합한 경우 오늘의 질문 답변 등록 성공")
        void 커플_재결합_후_오늘의_질문_답변_등록_성공() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            String answer = "my answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            // when & then
            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("커플 연결이 끊어진 이후 재결합한 경우 오늘의 질문 답변 수정 성공")
        void 커플_재결합_후_오늘의_질문_답변_수정_성공() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            String answer = "my answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            String newAnswer = "new answer";
            QuestionController.AnswerRequestDto newRequestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(newAnswer);

            // when & then
            mockMvc.perform(patch("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newRequestDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("커플 연결이 끊어진 이후 재결합한 경우 과거 질문 조회 성공")
        void 커플_재결합_후_과거_질문_조회_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            em.createQuery("UPDATE CoupleQuestionEntity cq SET cq.coupleQuestionState = :state, cq.bothAnsweredAt = :bothAnsweredAt WHERE cq.id = :id")
                    .setParameter("state", CoupleQuestionState.COMPLETED)
                    .setParameter("bothAnsweredAt", LocalDateTime.now().minusDays(1))
                    .setParameter("id", coupleQuestion.getId())
                    .executeUpdate();

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/questions/1")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("커플 연결이 끊어진 이후 다른 사람과 연결한 경우 오늘의 질문 조회 성공")
        void 다른_사람과_연결_후_오늘의_질문_조회_성공() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(other.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("커플 연결이 끊어진 이후 다른 사람과 연결한 경우 오늘의 질문 답변 등록 성공")
        void 다른_사람과_연결_후_오늘의_질문_답변_등록_성공() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(other.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            String answer = "my answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            // when & then
            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("커플 연결이 끊어진 이후 다른 사람과 연결한 경우 오늘의 질문 답변 수정 실패")
        void 다른_사람과_연결_후_오늘의_질문_답변_수정_실패() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            String answer = "my answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(other.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            String newAnswer = "new answer";
            QuestionController.AnswerRequestDto newRequestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(newAnswer);

            // when & then
            mockMvc.perform(patch("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newRequestDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }

        @Test
        @DisplayName("커플 연결이 끊어진 이후 다른 사람과 연결한 경우 과거 질문 조회 성공")
        void 다른_사람과_연결_후_과거_질문_조회_성공() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            em.createQuery("UPDATE CoupleQuestionEntity cq SET cq.coupleQuestionState = :state, cq.bothAnsweredAt = :bothAnsweredAt WHERE cq.id = :id")
                    .setParameter("state", CoupleQuestionState.COMPLETED)
                    .setParameter("bothAnsweredAt", LocalDateTime.now().minusDays(1))
                    .setParameter("id", coupleQuestion.getId())
                    .executeUpdate();

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(other.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/questions/1")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("커플 연결이 끊어진 이후 다른 사람과 연결한 경우 이전 커플 질문 조회 실패")
        void 다른_사람과_연결_후_이전_커플_질문_조회_실패() throws Exception {
            // given
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(other.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/questions/" + coupleQuestion.getId() + "/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(MEMBER_ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.code").value(MEMBER_ACCESS_DENIED.getCode()));
        }

        @Test
        @DisplayName("커플이 아닌 멤버가 오늘의 질문 답변 등록 후 커플 연동 시 답변 생성 확인")
        void 커플_아닌_멤버_답변_등록_후_커플_연동_시_답변_생성_확인() throws Exception {
            // given
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            String answer = "my answer";
            QuestionController.AnswerRequestDto requestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(answer);

            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk());

            // when
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // then
            MemberAnswerEntity memberAnswer = em.createQuery("select ma from MemberAnswerEntity ma where ma.memberEntityId.value = :memberId", MemberAnswerEntity.class)
                    .setParameter("memberId", member.getId())
                    .getSingleResult();
            Assertions.assertThat(memberAnswer.getAnswer()).isEqualTo(answer);
        }

        @Test
        @DisplayName("커플이 아닌 멤버가 각자 오늘의 질문 답변 등록 후 커플 연동 시 답변 생성 및 완료 상태 확인")
        void 각자_답변_등록_후_커플_연동_시_답변_생성_및_완료_상태_확인() throws Exception {
            // given
            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/questions/today")
                            .header("Authorization", "Bearer " + partnerAccessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            String myAnswer = "my answer";
            QuestionController.AnswerRequestDto myRequestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(myAnswer);

            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(myRequestDto)))
                    .andExpect(status().isOk());

            String partnerAnswer = "partner's answer";
            QuestionController.AnswerRequestDto partnerRequestDto = CoupleQuestionRequestDtoFactory.createAnswerRequestDto(partnerAnswer);

            mockMvc.perform(post("/questions/today/answers")
                            .header("Authorization", "Bearer " + partnerAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(partnerRequestDto)))
                    .andExpect(status().isOk());

            // when
            MvcResult coupleResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String coupleContent = coupleResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(coupleContent, "$.data.coupleId");

            // then
            MemberAnswerEntity memberAnswer = em.createQuery("select ma from MemberAnswerEntity ma where ma.memberEntityId.value = :memberId", MemberAnswerEntity.class)
                    .setParameter("memberId", member.getId())
                    .getSingleResult();
            Assertions.assertThat(memberAnswer.getAnswer()).isEqualTo(myAnswer);

            MemberAnswerEntity partnerMemberAnswer = em.createQuery("select ma from MemberAnswerEntity ma where ma.memberEntityId.value = :memberId", MemberAnswerEntity.class)
                    .setParameter("memberId", partner.getId())
                    .getSingleResult();
            Assertions.assertThat(partnerMemberAnswer.getAnswer()).isEqualTo(partnerAnswer);

            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();
            Assertions.assertThat(coupleQuestion.getCoupleQuestionState()).isEqualTo(CoupleQuestionState.COMPLETED);
        }
    }
}
