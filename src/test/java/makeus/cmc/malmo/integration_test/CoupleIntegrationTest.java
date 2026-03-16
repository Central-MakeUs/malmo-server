package makeus.cmc.malmo.integration_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatRoomEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.couple.CoupleEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.question.CoupleQuestionEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.application.port.out.member.GenerateTokenPort;
import makeus.cmc.malmo.domain.value.state.*;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static makeus.cmc.malmo.adaptor.in.exception.ErrorCode.*;
import static makeus.cmc.malmo.util.GlobalConstants.FIRST_QUESTION_LEVEL;
import static makeus.cmc.malmo.util.GlobalConstants.INIT_CHATROOM_LEVEL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class CoupleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
                .providerId("partnerProviderId")
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

    @Nested
    @DisplayName("커플 연결 기능 검증")
    class CoupleLinkFeature {
        @Test
        @DisplayName("정상적인 요청의 경우 커플 연결이 성공한다.")
        void 커플_연결_성공() throws Exception {
            // when
            MvcResult mvcResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseContent = mvcResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(responseContent, "$.data.coupleId");

            // then
            // 커플 생성 여부 확인
            Assertions.assertThat(coupleId).isNotNull();
            CoupleEntity couple = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.id = :coupleId", CoupleEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            Assertions.assertThat(couple.getCoupleState()).isEqualTo(CoupleState.ALIVE);
            Assertions.assertThat(couple.getStartLoveDate()).isEqualTo(LocalDate.now());

            List<Long> memberIds = List.of(couple.getFirstMemberId().getValue(), couple.getSecondMemberId().getValue());
            Assertions.assertThat(memberIds).containsExactlyInAnyOrder(member.getId(), partner.getId());

            // 커플 질문이 생성되었는지 확인
            CoupleQuestionEntity coupleQuestion = em.createQuery("SELECT cq FROM CoupleQuestionEntity cq WHERE cq.coupleEntityId.value = :coupleId", CoupleQuestionEntity.class)
                    .setParameter("coupleId", couple.getId())
                    .getSingleResult();
            Assertions.assertThat(coupleQuestion).isNotNull();
            Assertions.assertThat(coupleQuestion.getQuestion()).isNotNull();
            Assertions.assertThat(coupleQuestion.getQuestion().getLevel()).isEqualTo(FIRST_QUESTION_LEVEL);
            Assertions.assertThat(coupleQuestion.getCoupleQuestionState()).isEqualTo(CoupleQuestionState.ALIVE);
        }

//        @Test
//        @DisplayName("정지된 채팅방이 있는 경우 커플 연결이 성공 후 채팅방이 활성화된다.")
//        void 커플_연결_성공_채팅방_활성화() throws Exception {
//            // given
//            ChatRoomEntity memberChatRoom = ChatRoomEntity.builder()
//                    .memberEntityId(MemberEntityId.of(member.getId()))
//                    .chatRoomState(ChatRoomState.PAUSED)
//                    .level(INIT_CHATROOM_LEVEL)
//                    .build();
//
//            ChatRoomEntity partnerChatRoom = ChatRoomEntity.builder()
//                    .memberEntityId(MemberEntityId.of(partner.getId()))
//                    .chatRoomState(ChatRoomState.PAUSED)
//                    .level(INIT_CHATROOM_LEVEL)
//                    .build();
//
//            em.persist(memberChatRoom);
//            em.persist(partnerChatRoom);
//            em.flush();
//
//            // when
//            MvcResult mvcResult = mockMvc.perform(post("/couples")
//                            .header("Authorization", "Bearer " + accessToken)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(
//                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
//                            )))
//                    .andExpect(status().isOk())
//                    .andReturn();
//            String responseContent = mvcResult.getResponse().getContentAsString();
//            Integer coupleId = JsonPath.read(responseContent, "$.data.coupleId");
//
//            // then
//            // 커플 생성 여부 확인
//            Assertions.assertThat(coupleId).isNotNull();
//            CoupleEntity couple = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.id = :coupleId", CoupleEntity.class)
//                    .setParameter("coupleId", Long.valueOf(coupleId))
//                    .getSingleResult();
//            Assertions.assertThat(couple).isNotNull();
//            Assertions.assertThat(couple.getCoupleState()).isEqualTo(CoupleState.ALIVE);
//            Assertions.assertThat(couple.getStartLoveDate()).isEqualTo(partner.getStartLoveDate());
//
//            List<Long> memberIds = List.of(couple.getFirstMemberId().getValue(), couple.getSecondMemberId().getValue());
//            Assertions.assertThat(memberIds).containsExactlyInAnyOrder(member.getId(), partner.getId());
//
//            // 커플 멤버의 채팅방 상태가 활성화 되었는지 확인
//            ChatRoomEntity memberChatRoomAfter = em.createQuery("SELECT cr FROM ChatRoomEntity cr WHERE cr.memberEntityId.value = :memberId", ChatRoomEntity.class)
//                    .setParameter("memberId", member.getId())
//                    .getSingleResult();
//            ChatRoomEntity partnerChatRoomAfter = em.createQuery("SELECT cr FROM ChatRoomEntity cr WHERE cr.memberEntityId.value = :partnerId", ChatRoomEntity.class)
//                    .setParameter("partnerId", partner.getId())
//                    .getSingleResult();
//            Assertions.assertThat(memberChatRoomAfter.getChatRoomState()).isEqualTo(ChatRoomState.NEED_NEXT_QUESTION);
//            Assertions.assertThat(partnerChatRoomAfter.getChatRoomState()).isEqualTo(ChatRoomState.NEED_NEXT_QUESTION);
//        }

        @Test
        @DisplayName("재결합 커플인 경우 커플 연결이 성공 후 데이터가 복구된다.")
        void 재결합_커플_연결_성공_데이터_복구() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // when
            MvcResult mvcResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseContent = mvcResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(responseContent, "$.data.coupleId");

            // then
            CoupleEntity couple = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.id = :coupleId", CoupleEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            Assertions.assertThat(couple).isNotNull();
            Assertions.assertThat(couple.getCoupleState()).isEqualTo(CoupleState.ALIVE);
        }

        @Test
        @DisplayName("30일 이내 재연결 시 기존 커플이 복구된다.")
        void 삼십일_이내_재연결_기존_커플_복구() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // when - 즉시 재연결 (30일 이내)
            MvcResult mvcResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseContent = mvcResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(responseContent, "$.data.coupleId");

            // then
            CoupleEntity couple = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.id = :coupleId", CoupleEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            Assertions.assertThat(couple).isNotNull();
            Assertions.assertThat(couple.getCoupleState()).isEqualTo(CoupleState.ALIVE);
            Assertions.assertThat(couple.getDeletedAt()).isNull(); // 복구 시 deletedAt이 null로 초기화
        }

        @Test
        @DisplayName("30일 초과 재연결 시 새로운 커플이 생성된다.")
        void 삼십일_초과_재연결_새로운_커플_생성() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // deletedAt을 31일 전으로 수정
            em.createQuery("UPDATE CoupleEntity c SET c.deletedAt = :deletedAt WHERE c.coupleState = 'DELETED'")
                    .setParameter("deletedAt", java.time.LocalDateTime.now().minusDays(31))
                    .executeUpdate();
            em.flush();

            // when
            MvcResult mvcResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseContent = mvcResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(responseContent, "$.data.coupleId");

            // then
            CoupleEntity newCouple = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.id = :coupleId", CoupleEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();

            Assertions.assertThat(newCouple).isNotNull();
            Assertions.assertThat(newCouple.getCoupleState()).isEqualTo(CoupleState.ALIVE);
            Assertions.assertThat(newCouple.getDeletedAt()).isNull();

            // 기존 DELETED 커플이 여전히 존재하는지 확인
            List<CoupleEntity> deletedCouples = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.coupleState = 'DELETED'", CoupleEntity.class)
                    .getResultList();
            Assertions.assertThat(deletedCouples).hasSize(1);
            Assertions.assertThat(deletedCouples.get(0).getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("커플 해지 시 deletedAt이 현재 시간으로 설정된다.")
        void 커플_해지_시_deletedAt_설정() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when
            LocalDateTime beforeUnlink = LocalDateTime.now();
            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());
            LocalDateTime afterUnlink = LocalDateTime.now();

            // then
            CoupleEntity deletedCouple = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.coupleState = 'DELETED'", CoupleEntity.class)
                    .getSingleResult();

            Assertions.assertThat(deletedCouple.getDeletedAt()).isNotNull();
            Assertions.assertThat(deletedCouple.getDeletedAt()).isAfterOrEqualTo(beforeUnlink);
            Assertions.assertThat(deletedCouple.getDeletedAt()).isBeforeOrEqualTo(afterUnlink);
        }

        @Test
        @DisplayName("탈퇴한 사용자의 경우 커플 연결이 실패한다.")
        void 탈퇴한_사용자_커플_연결_실패() throws Exception {
            // given
            MemberEntity deletedPartner = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("partnerProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.DELETED)
                    .email("testEmail2@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("inviteD"))
                    .build();
            em.persist(deletedPartner);
            em.flush();

            // when & then
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(deletedPartner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_COUPLE_CODE.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_COUPLE_CODE.getCode()));
        }

        @Test
        @DisplayName("이미 커플인 사용자의 경우 커플 연결이 실패한다.")
        void 이미_커플인_사용자_커플_연결_실패() throws Exception {
            // given
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(other.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(ALREADY_COUPLED_MEMBER.getMessage()))
                    .andExpect(jsonPath("code").value(ALREADY_COUPLED_MEMBER.getCode()));
        }

        @Test
        @DisplayName("초대 코드가 이미 사용된 경우 커플 연결이 실패한다.")
        void 초대_코드_이미_사용된_경우_커플_연결_실패() throws Exception {
            // given : partner가 other와 커플 연결된 상태
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + partnerAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(other.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(ALREADY_COUPLED_MEMBER.getMessage()))
                    .andExpect(jsonPath("code").value(ALREADY_COUPLED_MEMBER.getCode()));
        }

        @Test
        @DisplayName("초대 코드가 없는 경우 커플 연결이 실패한다.")
        void 초대_코드_없는_경우_커플_연결_실패() throws Exception {
            // when & then
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto("notExist")
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_COUPLE_CODE.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_COUPLE_CODE.getCode()));
        }

        @Test
        @DisplayName("초대 코드 길이가 9자리인 경우 커플 연결이 실패한다.")
        void 초대_코드_길이_9자리인_경우_커플_연결_실패() throws Exception {
            // when & then
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto("BigInvite")
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("내 초대 코드로 커플 연결 시도 시 실패한다.")
        void 내_초대_코드로_커플_연결_시도_실패() throws Exception {
            // when & then
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(member.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NOT_VALID_COUPLE_CODE.getMessage()))
                    .andExpect(jsonPath("code").value(NOT_VALID_COUPLE_CODE.getCode()));
        }
    }

    @Nested
    @DisplayName("커플 연결 끊기 기능 검증")
    class CoupleUnLinkFeature {
        @Test
        @DisplayName("정상적인 요청의 경우 커플 연결이 끊어진다.")
        void 커플_연결_끊기_성공() throws Exception {
            // given
            MvcResult mvcResult = mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseContent = mvcResult.getResponse().getContentAsString();
            Integer coupleId = JsonPath.read(responseContent, "$.data.coupleId");

            // when
            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // then
            CoupleEntity couple = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.id = :coupleId", CoupleEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();
            Assertions.assertThat(couple.getCoupleState()).isEqualTo(CoupleState.DELETED);

            em.flush();
            em.clear();

            MemberEntity unlinkedMember = em.find(MemberEntity.class, member.getId());
            Assertions.assertThat(unlinkedMember.getCoupleEntityId()).isNull();

            // 커플 전용 API 접근 시 실패
            mockMvc.perform(get("/members/partner")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_PARTNER_PROFILE.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_PARTNER_PROFILE.getCode()));
        }

        @Test
        @DisplayName("커플이 아닌 사용자가 커플 연결 끊기를 시도하면 실패한다.")
        void 커플이_아닌_사용자_커플_연결_끊기_실패() throws Exception {
            // when & then
            mockMvc.perform(delete("/couples")
                            .header("Authorization", "Bearer " + generateTokenPort.generateToken(other.getId(), other.getMemberRole()).getAccessToken()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("message").value(NOT_COUPLE_MEMBER.getMessage()))
                    .andExpect(jsonPath("code").value(NOT_COUPLE_MEMBER.getCode()));
        }

    }


}
