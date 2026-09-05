package makeus.cmc.malmo.integration_test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import makeus.cmc.malmo.adaptor.in.web.controller.SignUpController;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.adaptor.out.oauth.KakaoUnlinkAdapter;
import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.ChatRoomEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.couple.CoupleEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.question.CoupleQuestionEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.question.QuestionEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.terms.MemberTermsAgreementEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.terms.TermsEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.CoupleEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.adaptor.in.exception.ErrorCode;
import makeus.cmc.malmo.application.port.out.member.GenerateTokenPort;
import makeus.cmc.malmo.domain.value.state.*;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;
import makeus.cmc.malmo.domain.value.type.TermsType;
import makeus.cmc.malmo.integration_test.dto_factory.CoupleRequestDtoFactory;
import makeus.cmc.malmo.integration_test.dto_factory.LoveTypeQuestionRequestDtoFactory;
import makeus.cmc.malmo.integration_test.dto_factory.MemberRequestDtoFactory;
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
import java.util.Map;

import static makeus.cmc.malmo.adaptor.in.exception.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class MemberIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private GenerateTokenPort generateTokenPort;

    @MockBean
    private KakaoUnlinkAdapter kakaoUnlinkAdapter;

    private String accessToken;

    private MemberEntity member;
    
    private LocalDate newDday = LocalDate.now().minusDays(100);

    @BeforeEach
    void setup() {
        member = MemberEntity.builder()
                .provider(Provider.KAKAO)
                .providerId("testProviderId")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .email("testEmail@test.com")
                .nickname("nickname")
                .inviteCodeEntityValue(InviteCodeEntityValue.of("testInviteCode"))
                .build();

        em.persist(member);
        em.flush();

        TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());

        accessToken = tokenInfo.getAccessToken();
    }

    @Nested
    @DisplayName("온보딩 API 테스트")
    class SignUpV2Test {

        private String accessToken;
        private MemberEntity member;
        private TermsEntity terms;

        @BeforeEach
        void setup() {
            // 약관 생성
            terms = TermsEntity.builder()
                    .termsType(TermsType.SERVICE_USAGE)
                    .content("서비스 이용약관")
                    .isRequired(true)
                    .build();
            em.persist(terms);

            // 미가입 상태의 멤버 생성 (OAuth 인증만 완료)
            member = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("signUpV2ProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.BEFORE_ONBOARDING)
                    .email("test@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("TEST1234"))
                    .build();

            em.persist(member);
            em.flush();

            TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());
            accessToken = tokenInfo.getAccessToken();
        }

        @Test
        @DisplayName("온보딩 - startLoveDate 없이 회원가입 성공")
        void signUpV2WithoutStartLoveDate() throws Exception {
            // given
            Map<String, Object> requestDto = Map.of(
                    "nickname", "테스트유저",
                    "terms", List.of(
                            Map.of("termsId", terms.getId(), "isAgreed", true)
                    )
            );

            // when & then
            mockMvc.perform(post("/members/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 회원가입 후 확인
            em.flush();
            em.clear();

            MemberEntity savedMember = em.find(MemberEntity.class, member.getId());
            assertThat(savedMember.getNickname()).isEqualTo("테스트유저");
            assertThat(savedMember.getMemberState()).isEqualTo(MemberState.ALIVE);
            // V2에서는 개인의 startLoveDate가 null이어야 함
            assertThat(savedMember.getStartLoveDate()).isNull();
        }
    }

    @Nested
    @DisplayName("커플 연동 시 startLoveDate 당일 초기화 테스트")
    class CoupleLinkTest {

        private String accessToken;
        private MemberEntity member;
        private MemberEntity partner;

        @BeforeEach
        void setup() {
            // 회원1 생성 (회원가입 완료)
            member = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("coupleLinkProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .nickname("테스트유저1")
                    .email("test1@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("TEST1234"))
                    .build();

            // 회원2 (파트너) 생성
            partner = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("partnerProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .nickname("파트너")
                    .email("partner@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("PARTNER1"))
                    .build();

            em.persist(member);
            em.persist(partner);
            em.flush();

            TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());
            accessToken = tokenInfo.getAccessToken();
        }

        @Test
        @DisplayName("커플 연동 시 startLoveDate는 당일로 초기화")
        void coupleLinkInitializesStartLoveDateToToday() throws Exception {
            // given
            LocalDate today = LocalDate.now();
            Map<String, Object> requestDto = Map.of(
                    "coupleCode", "PARTNER1"
            );

            // when - 멤버가 파트너의 초대코드로 커플 연동
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // then
            em.flush();
            em.clear();

            MemberEntity savedMember = em.find(MemberEntity.class, member.getId());
            CoupleEntity couple = em.find(CoupleEntity.class, savedMember.getCoupleEntityId().getValue());

            // 커플의 startLoveDate가 당일인지 확인
            assertThat(couple.getStartLoveDate()).isEqualTo(today);
            assertThat(couple.getCoupleState()).isEqualTo(CoupleState.ALIVE);
        }
    }

    @Nested
    @DisplayName("디데이 변경 API 테스트")
    class UpdateStartLoveDateV2Test {

        private String accessToken;
        private String nonCoupleAccessToken;
        private MemberEntity member;
        private MemberEntity partner;
        private MemberEntity nonCoupleMember;
        private CoupleEntity couple;

        @BeforeEach
        void setup() {
            // 커플 회원1
            member = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("updateStartLoveDateProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .nickname("테스트유저1")
                    .email("test1@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("TEST1234"))
                    .loveTypeCategory(LoveTypeCategory.STABLE_TYPE)
                    .build();

            // 커플 회원2 (파트너)
            partner = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("partnerProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .nickname("파트너")
                    .email("partner@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("PARTNER1"))
                    .loveTypeCategory(LoveTypeCategory.STABLE_TYPE)
                    .build();

            // 커플이 아닌 회원
            nonCoupleMember = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("nonCoupleProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .nickname("솔로유저")
                    .email("solo@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("SOLO123"))
                    .build();

            em.persist(member);
            em.persist(partner);
            em.persist(nonCoupleMember);
            em.flush();

            // 커플 생성
            couple = CoupleEntity.builder()
                    .startLoveDate(LocalDate.now())
                    .coupleState(CoupleState.ALIVE)
                    .isStartLoveDateUpdated(false)
                    .firstMemberId(makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId.of(member.getId()))
                    .secondMemberId(makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId.of(partner.getId()))
                    .build();

            em.persist(couple);
            em.flush();

            // 커플 연결 (새로운 Entity로 생성)
            MemberEntity updatedMember = em.find(MemberEntity.class, member.getId());
            MemberEntity memberWithCouple = MemberEntity.builder()
                    .id(updatedMember.getId())
                    .provider(updatedMember.getProvider())
                    .providerId(updatedMember.getProviderId())
                    .memberRole(updatedMember.getMemberRole())
                    .memberState(updatedMember.getMemberState())
                    .nickname(updatedMember.getNickname())
                    .email(updatedMember.getEmail())
                    .inviteCodeEntityValue(updatedMember.getInviteCodeEntityValue())
                    .loveTypeCategory(updatedMember.getLoveTypeCategory())
                    .coupleEntityId(CoupleEntityId.of(couple.getId()))
                    .build();
            em.merge(memberWithCouple);

            MemberEntity updatedPartner = em.find(MemberEntity.class, partner.getId());
            MemberEntity partnerWithCouple = MemberEntity.builder()
                    .id(updatedPartner.getId())
                    .provider(updatedPartner.getProvider())
                    .providerId(updatedPartner.getProviderId())
                    .memberRole(updatedPartner.getMemberRole())
                    .memberState(updatedPartner.getMemberState())
                    .nickname(updatedPartner.getNickname())
                    .email(updatedPartner.getEmail())
                    .inviteCodeEntityValue(updatedPartner.getInviteCodeEntityValue())
                    .loveTypeCategory(updatedPartner.getLoveTypeCategory())
                    .coupleEntityId(CoupleEntityId.of(couple.getId()))
                    .build();
            em.merge(partnerWithCouple);

            em.flush();
            em.clear();

            TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());
            accessToken = tokenInfo.getAccessToken();

            TokenInfo nonCoupleToken = generateTokenPort.generateToken(nonCoupleMember.getId(), nonCoupleMember.getMemberRole());
            nonCoupleAccessToken = nonCoupleToken.getAccessToken();
        }

        @Test
        @DisplayName("디데이 변경 - 커플은 연애 시작일 변경 가능")
        void updateStartLoveDateV2ForCouple() throws Exception {
            // given
            LocalDate newStartDate = LocalDate.of(2024, 1, 1);
            Map<String, Object> requestDto = Map.of(
                    "startLoveDate", newStartDate.toString()
            );

            // when
            mockMvc.perform(patch("/members/start-love-date")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.startLoveDate").value(newStartDate.toString()));

            // then - 커플의 startLoveDate만 변경되었는지 확인
            em.flush();
            em.clear();

            CoupleEntity updatedCouple = em.find(CoupleEntity.class, couple.getId());
            assertThat(updatedCouple.getStartLoveDate()).isEqualTo(newStartDate);

            // 개인의 startLoveDate는 null 유지
            MemberEntity updatedMember = em.find(MemberEntity.class, member.getId());
            assertThat(updatedMember.getStartLoveDate()).isNull();
        }

        @Test
        @DisplayName("디데이 변경 - 커플이 아닌 사용자는 실패")
        void updateStartLoveDateV2ForNonCoupleFails() throws Exception {
            // given
            LocalDate newStartDate = LocalDate.of(2024, 1, 1);
            Map<String, Object> requestDto = Map.of(
                    "startLoveDate", newStartDate.toString()
            );

            // when & then - 커플이 아닌 사용자는 403 Forbidden
            mockMvc.perform(patch("/members/start-love-date")
                            .header("Authorization", "Bearer " + nonCoupleAccessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden()); // @CheckCoupleMember가 403 반환
        }
    }

    @Nested
    @DisplayName("회원 정보 조회 시 커플의 startLoveDate 반환 테스트")
    class GetMemberInfoTest {

        private String accessToken;
        private String nonCoupleAccessToken;
        private MemberEntity member;
        private MemberEntity partner;
        private MemberEntity nonCoupleMember;
        private CoupleEntity couple;

        @BeforeEach
        void setup() {
            // 커플 회원1
            member = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("getMemberInfoProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .nickname("테스트유저1")
                    .email("test1@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("TEST1234"))
                    .loveTypeCategory(LoveTypeCategory.STABLE_TYPE)
                    .build();

            // 커플 회원2 (파트너)
            partner = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("partnerProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .nickname("파트너")
                    .email("partner@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("PARTNER1"))
                    .loveTypeCategory(LoveTypeCategory.STABLE_TYPE)
                    .build();

            // 커플이 아닌 회원
            nonCoupleMember = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId("nonCoupleProviderId")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.ALIVE)
                    .nickname("솔로유저")
                    .email("solo@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("SOLO123"))
                    .build();

            em.persist(member);
            em.persist(partner);
            em.persist(nonCoupleMember);
            em.flush();

            // 커플 생성 (특정 날짜로 설정)
            LocalDate coupleStartDate = LocalDate.of(2024, 5, 1);
            couple = CoupleEntity.builder()
                    .startLoveDate(coupleStartDate)
                    .coupleState(CoupleState.ALIVE)
                    .isStartLoveDateUpdated(false)
                    .firstMemberId(makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId.of(member.getId()))
                    .secondMemberId(makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId.of(partner.getId()))
                    .build();

            em.persist(couple);
            em.flush();

            // 커플 연결
            MemberEntity updatedMember = em.find(MemberEntity.class, member.getId());
            MemberEntity memberWithCouple = MemberEntity.builder()
                    .id(updatedMember.getId())
                    .provider(updatedMember.getProvider())
                    .providerId(updatedMember.getProviderId())
                    .memberRole(updatedMember.getMemberRole())
                    .memberState(updatedMember.getMemberState())
                    .nickname(updatedMember.getNickname())
                    .email(updatedMember.getEmail())
                    .inviteCodeEntityValue(updatedMember.getInviteCodeEntityValue())
                    .loveTypeCategory(updatedMember.getLoveTypeCategory())
                    .coupleEntityId(CoupleEntityId.of(couple.getId()))
                    .build();
            em.merge(memberWithCouple);

            em.flush();
            em.clear();

            TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());
            accessToken = tokenInfo.getAccessToken();

            TokenInfo nonCoupleToken = generateTokenPort.generateToken(nonCoupleMember.getId(), nonCoupleMember.getMemberRole());
            nonCoupleAccessToken = nonCoupleToken.getAccessToken();
        }

        @Test
        @DisplayName("회원 정보 조회 - 커플의 startLoveDate 반환")
        void getMemberInfoReturnsCoupleStartLoveDate() throws Exception {
            // given
            LocalDate coupleStartDate = LocalDate.of(2024, 5, 1);

            // when & then
            mockMvc.perform(get("/members")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.startLoveDate").value(coupleStartDate.toString()));
        }

        @Test
        @DisplayName("회원 정보 조회 - 커플이 아닌 경우 startLoveDate 없음")
        void getMemberInfoReturnsNullForNonCouple() throws Exception {
            // when & then
            mockMvc.perform(get("/members")
                            .header("Authorization", "Bearer " + nonCoupleAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.startLoveDate").doesNotExist());
        }
    }

    @Nested
    @DisplayName("회원가입 기능 검증 (기존 정책)")
    class SignUpFeature {
        @Test
        @DisplayName("정상적인 요청의 경우 회원가입이 성공한다")
        void 회원가입_성공() throws Exception {
            List<SignUpController.TermsDto> terms = List.of(
                    MemberRequestDtoFactory.createTermsDto(1L, true),
                    MemberRequestDtoFactory.createTermsDto(2L, true),
                    MemberRequestDtoFactory.createTermsDto(3L, true),
                    MemberRequestDtoFactory.createTermsDto(4L, true)
            );

            mockMvc.perform(post("/members/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createSignUpRequestDto(terms,
                                            "테스트닉네임"))))
                    .andExpect(status().isOk());

            MemberEntity savedMember = em.find(MemberEntity.class, member.getId());

            Assertions.assertThat(savedMember.getNickname()).isEqualTo("테스트닉네임");

            List<MemberTermsAgreementEntity> agreements = em.createQuery(
                            "SELECT t FROM MemberTermsAgreementEntity t WHERE t.memberEntityId.value = :memberId",
                            MemberTermsAgreementEntity.class)
                    .setParameter("memberId", member.getId())
                    .getResultList();

            Assertions.assertThat(agreements).hasSize(4);
            Assertions.assertThat(agreements)
                    .allMatch(MemberTermsAgreementEntity::isAgreed)
                    .extracting(agreement -> agreement.getTermsEntityId().getValue())
                    .containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        }

        // 닉네임 길이 경계값 테스트
        @Test
        @DisplayName("닉네임 10자인 경우 회원가입이 성공한다")
        void 회원가입_닉네임_길이_경계값_성공() throws Exception {
            List<SignUpController.TermsDto> terms = List.of(
                    MemberRequestDtoFactory.createTermsDto(1L, true),
                    MemberRequestDtoFactory.createTermsDto(2L, true),
                    MemberRequestDtoFactory.createTermsDto(3L, true),
                    MemberRequestDtoFactory.createTermsDto(4L, true)
            );

            mockMvc.perform(post("/members/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createSignUpRequestDto(terms,
                                            "테스트닉네임1234")) // 10자
                            ))
                    .andExpect(status().isOk());
        }

        // 닉네임 길이 초과 실패 테스트
        @Test
        @DisplayName("닉네임이 11자 이상인 경우 회원가입이 실패한다")
        void 회원가입_닉네임_길이_초과_실패() throws Exception {
            List<SignUpController.TermsDto> terms = List.of(
                    MemberRequestDtoFactory.createTermsDto(1L, true),
                    MemberRequestDtoFactory.createTermsDto(2L, true),
                    MemberRequestDtoFactory.createTermsDto(3L, true),
                    MemberRequestDtoFactory.createTermsDto(4L, true)
            );

            mockMvc.perform(post("/members/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createSignUpRequestDto(terms,
                                            "테스트닉네임12345")) // 11자
                            ))
                    .andExpect(status().isBadRequest());
        }

        // 닉네임 특수문자 실패 테스트
        @Test
        @DisplayName("닉네임에 특수문자가 포함된 경우 회원가입이 실패한다")
        void 회원가입_닉네임_특수문자_포함_실패() throws Exception {
            List<SignUpController.TermsDto> terms = List.of(
                    MemberRequestDtoFactory.createTermsDto(1L, true),
                    MemberRequestDtoFactory.createTermsDto(2L, true),
                    MemberRequestDtoFactory.createTermsDto(3L, true),
                    MemberRequestDtoFactory.createTermsDto(4L, true)
            );

            mockMvc.perform(post("/members/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createSignUpRequestDto(terms,
                                            "테스트닉네임!@#"))) // 특수문자 포함
                            )
                    .andExpect(status().isBadRequest());
        }

        // 시작일 경계값 테스트
        @Test
        @DisplayName("시작일이 오늘 날짜인 경우 회원가입이 성공한다")
        void 회원가입_시작일_오늘_성공() throws Exception {
            List<SignUpController.TermsDto> terms = List.of(
                    MemberRequestDtoFactory.createTermsDto(1L, true),
                    MemberRequestDtoFactory.createTermsDto(2L, true),
                    MemberRequestDtoFactory.createTermsDto(3L, true),
                    MemberRequestDtoFactory.createTermsDto(4L, true)
            );

            mockMvc.perform(post("/members/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createSignUpRequestDto(terms,
                                            "테스트닉네임")
                            )))
                    .andExpect(status().isOk());
        }

        // 시작일 경계값 이후 실패 테스트
        @Test
        @DisplayName("시작일이 미래 날짜인 경우 회원가입이 실패한다")
        void 회원가입_시작일_미래_실패() throws Exception {
            // V2 정책에서는 startLoveDate를 회원가입 시 설정하지 않으므로 이 테스트는 더 이상 유효하지 않음
            // 대신 V2 정책에 맞는 테스트로 변경
            List<SignUpController.TermsDto> terms = List.of(
                    MemberRequestDtoFactory.createTermsDto(1L, true),
                    MemberRequestDtoFactory.createTermsDto(2L, true),
                    MemberRequestDtoFactory.createTermsDto(3L, true),
                    MemberRequestDtoFactory.createTermsDto(4L, true)
            );

            mockMvc.perform(post("/members/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createSignUpRequestDto(terms,
                                            "테스트닉네임")
                            )))
                    .andExpect(status().isOk()); // V2에서는 startLoveDate 없이 회원가입 성공
        }

        @Test
        @DisplayName("이전에 애착유형 검사를 한 경우 회원가입이 성공한다")
        void 회원가입_애착유형_검사_성공() throws Exception {
            // given
            int[] scores = {1, 1, 5, 1, 5, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1};

            MvcResult mvcResult = mockMvc.perform(post("/love-types/result")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoveTypeQuestionRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk())
                    .andReturn();

            String content = mvcResult.getResponse().getContentAsString();
            Integer loveTypeId = JsonPath.read(content, "$.data.loveTypeId");

            List<SignUpController.TermsDto> terms = List.of(
                    MemberRequestDtoFactory.createTermsDto(1L, true),
                    MemberRequestDtoFactory.createTermsDto(2L, true),
                    MemberRequestDtoFactory.createTermsDto(3L, true),
                    MemberRequestDtoFactory.createTermsDto(4L, true)
            );

            LocalDate today = LocalDate.now();

            // when
            mockMvc.perform(post("/members/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createSignUpWithLoveTypeIdRequestDto(terms,
                                            "테스트닉네임",
                                            Long.valueOf(loveTypeId))
                            )))
                    .andExpect(status().isOk());

            MemberEntity savedMember = em.find(MemberEntity.class, member.getId());

            Assertions.assertThat(savedMember.getNickname()).isEqualTo("테스트닉네임");
            // V2 정책에서는 개인의 startLoveDate가 null이어야 함
            Assertions.assertThat(savedMember.getStartLoveDate()).isNull();
            Assertions.assertThat(savedMember.getLoveTypeCategory()).isEqualTo(LoveTypeCategory.STABLE_TYPE);
            Assertions.assertThat(savedMember.getAnxietyRate()).isEqualTo(1.00f);
            Assertions.assertThat(savedMember.getAvoidanceRate()).isEqualTo(1.00f);
        }

        @Test
        @DisplayName("애착유형 검사 ID가 존재하지 않는 경우에도 회원가입이 성공한다")
        void 회원가입_애착유형_검사_ID_존재하지_않는_경우_성공() throws Exception {
            // given
            List<SignUpController.TermsDto> terms = List.of(
                    MemberRequestDtoFactory.createTermsDto(1L, true),
                    MemberRequestDtoFactory.createTermsDto(2L, true),
                    MemberRequestDtoFactory.createTermsDto(3L, true),
                    MemberRequestDtoFactory.createTermsDto(4L, true)
            );

            LocalDate today = LocalDate.now();
            long loveTypeId = 999L; // 존재하지 않는 ID

            // when
            mockMvc.perform(post("/members/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createSignUpWithLoveTypeIdRequestDto(terms,
                                            "테스트닉네임",
                                            loveTypeId)
                            )))
                    .andExpect(status().isOk());

            MemberEntity savedMember = em.find(MemberEntity.class, member.getId());

            Assertions.assertThat(savedMember.getNickname()).isEqualTo("테스트닉네임");
            // V2 정책에서는 개인의 startLoveDate가 null이어야 함
            Assertions.assertThat(savedMember.getStartLoveDate()).isNull();
            Assertions.assertThat(savedMember.getLoveTypeCategory()).isNull();
            Assertions.assertThat(savedMember.getAnxietyRate()).isEqualTo(0.0f);
            Assertions.assertThat(savedMember.getAvoidanceRate()).isEqualTo(0.0f);
        }
    }

    @Nested
    @DisplayName("멤버 탈퇴 검증")
    class MemberDeleteFeature {
        @Test
        @DisplayName("정상적인 요청의 경우 멤버 탈퇴가 성공한다")
        void 멤버_탈퇴_성공() throws Exception {
            // given
            doNothing().when(kakaoUnlinkAdapter).unlink("testProviderId");

            // when
            mockMvc.perform(delete("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // then
            MemberEntity deletedMember = em.find(MemberEntity.class, member.getId());
            Assertions.assertThat(deletedMember.getMemberState()).isEqualTo(MemberState.DELETED);
            Assertions.assertThat(deletedMember.getProviderId())
                    .isEqualTo("testProviderId_deleted_member_" + deletedMember.getId());
            verify(kakaoUnlinkAdapter, times(1)).unlink("testProviderId");
        }

        @Test
        @DisplayName("커플인 멤버의 경우 멤버 탈퇴 시 커플 상태가 DELETED로 변경된다")
        void 멤버_탈퇴_커플_상태_변경() throws Exception {
            // given
            MemberEntity partner = createAndSavePartner();
            doNothing().when(kakaoUnlinkAdapter).unlink("testProviderId");

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
            mockMvc.perform(delete("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // then
            MemberEntity deletedMember = em.find(MemberEntity.class, member.getId());
            Assertions.assertThat(deletedMember.getMemberState()).isEqualTo(MemberState.DELETED);

            CoupleEntity couple = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.id = :coupleId", CoupleEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();
            Assertions.assertThat(couple.getCoupleState()).isEqualTo(CoupleState.DELETED);
            verify(kakaoUnlinkAdapter, times(1)).unlink("testProviderId");
        }
    }

    @Nested
    @DisplayName("멤버 정보 조회 검증 (기존 정책)")
    class MemberInfoFeature {

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public static class ResponseDto <T> {
            String requestId;
            boolean success;
            String message;
            T data;
        }

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public static class MemberResponseDto {
            MemberState memberState;
            Provider provider;
            LocalDate startLoveDate;

            LoveTypeCategory loveTypeCategory;

            int totalChatRoomCount;
            int totalCoupleQuestionCount;

            float avoidanceRate;
            float anxietyRate;
            String nickname;
            String email;

            String relationshipStatus;
            String personalityType;
            String otherPersonalityType;
            String partnerLoveTypeCategory;
        }

        @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
        public static class PartnerResponseDto {
            private String personalityType;
            private String loveTypeCategory;
            private String description;
        }

        void assertMemberInfo(MemberResponseDto memberResponse, MemberEntity member, LocalDate startLoveDate, int coupleQuestionCount, int totalChatRoomCount) {
            Assertions.assertThat(memberResponse.memberState).isEqualTo(member.getMemberState());
            Assertions.assertThat(memberResponse.provider).isEqualTo(member.getProvider());
            // V2 정책: 실제 구현에서는 coupleEntity.startLoveDate.coalesce(memberEntity.startLoveDate)를 사용
            // 커플인 경우: 커플의 startLoveDate 사용, 솔로인 경우: null
            if (member.getCoupleEntityId() != null) {
                // 커플인 경우: 커플의 startLoveDate 사용
                Assertions.assertThat(memberResponse.startLoveDate).isEqualTo(startLoveDate);
            } else {
                // 솔로인 경우: startLoveDate 없음 (V2 정책)
                Assertions.assertThat(memberResponse.startLoveDate).isNull();
            }
            Assertions.assertThat(memberResponse.loveTypeCategory).isEqualTo(member.getLoveTypeCategory());
            Assertions.assertThat(memberResponse.totalCoupleQuestionCount).isEqualTo(coupleQuestionCount);
            Assertions.assertThat(memberResponse.totalChatRoomCount).isEqualTo(totalChatRoomCount);
            Assertions.assertThat(memberResponse.avoidanceRate).isEqualTo(member.getAvoidanceRate());
            Assertions.assertThat(memberResponse.anxietyRate).isEqualTo(member.getAnxietyRate());
            Assertions.assertThat(memberResponse.nickname).isEqualTo(member.getNickname());
            Assertions.assertThat(memberResponse.email).isEqualTo(member.getEmail());
            Assertions.assertThat(memberResponse.personalityType).isEqualTo(member.getPersonalityType());
            Assertions.assertThat(memberResponse.otherPersonalityType).isEqualTo(member.getOtherPersonalityType());
            Assertions.assertThat(memberResponse.partnerLoveTypeCategory)
                    .isEqualTo(member.getPartnerLoveTypeCategory() == null ? null : member.getPartnerLoveTypeCategory().name());
        }

        @Test
        @DisplayName("멤버 정보 조회 성공")
        void 멤버_정보_조회_성공() throws Exception {
            // when
            MvcResult mvcResult = mockMvc.perform(get("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<MemberResponseDto> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            // 멤버 정보가 정상적으로 조회되었는지 검증 (V2 정책: 솔로는 startLoveDate 없음)
            assertMemberInfo(responseDto.data, member, null, 0, 0);
        }

        @Test
        @DisplayName("커플 멤버의 경우 멤버 정보 조회 성공")
        void 커플_멤버_정보_조회_성공() throws Exception {
            // given
            MemberEntity partner = createAndSavePartner();

            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when
            MvcResult mvcResult = mockMvc.perform(get("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<MemberResponseDto> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            // V2 정책: 커플 연동 시 startLoveDate를 당일로 초기화
            assertMemberInfo(responseDto.data, member, LocalDate.now(), 0, 0);
        }

        @Test
        @DisplayName("완료된 채팅방 수와 커플 질문 수 조회 성공")
        void 완료된_채팅방_수와_커플_질문_수_조회_성공() throws Exception {
            // given
            MemberEntity partner = createAndSavePartner();

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

            // 커플 완료 질문 3개 생성
            // 현재 질문 종료
            em.createQuery("update CoupleQuestionEntity c SET c.coupleQuestionState = 'OUTDATED' where c.coupleEntityId.value = :coupleId")
                    .setParameter("coupleId", coupleId)
                    .executeUpdate();
            em.flush();
            QuestionEntity questionEntity = em.find(QuestionEntity.class, 1L);
            createAndSaveCoupleQuestion(questionEntity, coupleId);
            createAndSaveCoupleQuestion(questionEntity, coupleId);

            // 멤버 완료된 채팅방 3개 생성
            createAndSaveChatRoom();
            createAndSaveChatRoom();
            createAndSaveChatRoom();

            // when
            MvcResult mvcResult = mockMvc.perform(get("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<MemberResponseDto> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            // V2 정책: 커플 연동 시 startLoveDate를 당일로 초기화
            assertMemberInfo(responseDto.data, member, LocalDate.now(), 3, 3);
        }


        @Test
        @DisplayName("탈퇴한 멤버의 경우 멤버 정보 조회 실패")
        void 탈퇴한_멤버_정보_조회_실패() throws Exception {
            // given
            mockMvc.perform(delete("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_MEMBER.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_MEMBER.getCode()));
        }

        @Test
        @DisplayName("상대 프로필 등록 및 조회 성공")
        void 파트너_멤버_정보_조회_성공() throws Exception {
            // given
            Map<String, Object> requestDto = Map.of(
                    "personalityType", "enfp",
                    "loveTypeCategory", "CONFUSION_TYPE"
            );

            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.personalityType").value("ENFP"))
                    .andExpect(jsonPath("$.data.loveTypeCategory").value("CONFUSION_TYPE"))
                    .andExpect(jsonPath("$.data.description").value("혼란형"));

            // when
            MvcResult mvcResult = mockMvc.perform(get("/members/partner")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<PartnerResponseDto> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            // 상대 프로필이 정상적으로 조회되는지 검증
            PartnerResponseDto partnerDto = responseDto.data;
            Assertions.assertThat(partnerDto.personalityType).isEqualTo("ENFP");
            Assertions.assertThat(partnerDto.loveTypeCategory).isEqualTo("CONFUSION_TYPE");
            Assertions.assertThat(partnerDto.description).isEqualTo("혼란형");
        }

        @Test
        @DisplayName("상대 프로필 수정 성공")
        void 상대_프로필_수정_성공() throws Exception {
            // given
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("personalityType", "enfp"))))
                    .andExpect(status().isOk());

            // loveTypeCategory를 null로 전달하면 기존 값(null) 유지
            mockMvc.perform(patch("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"personalityType":"intj","loveTypeCategory":null}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.personalityType").value("INTJ"))
                    .andExpect(jsonPath("$.data.loveTypeCategory").doesNotExist())
                    .andExpect(jsonPath("$.data.description").doesNotExist());

            // when
            MvcResult mvcResult = mockMvc.perform(get("/members/partner")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            ResponseDto<PartnerResponseDto> responseDto = objectMapper.readValue(
                    responseContent,
                    new TypeReference<>() {}
            );

            PartnerResponseDto partnerDto = responseDto.data;
            Assertions.assertThat(partnerDto.personalityType).isEqualTo("INTJ");
            Assertions.assertThat(partnerDto.loveTypeCategory).isNull();
            Assertions.assertThat(partnerDto.description).isNull();
        }

        @Test
        @DisplayName("상대 프로필 수정 성공 - personalityType만 전달 시 loveTypeCategory 유지")
        void 상대_프로필_수정_성공_personalityType만_업데이트() throws Exception {
            // given - personalityType + loveTypeCategory 모두 등록
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"personalityType":"enfp","loveTypeCategory":"CONFUSION_TYPE"}
                                    """))
                    .andExpect(status().isOk());

            // when - personalityType만 수정 (loveTypeCategory 미포함)
            mockMvc.perform(patch("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"personalityType":"intj"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.personalityType").value("INTJ"))
                    .andExpect(jsonPath("$.data.loveTypeCategory").value("CONFUSION_TYPE"));
        }

        @Test
        @DisplayName("상대 프로필 수정 성공 - loveTypeCategory만 전달 시 personalityType 유지")
        void 상대_프로필_수정_성공_loveTypeCategory만_업데이트() throws Exception {
            // given - personalityType + loveTypeCategory 모두 등록
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"personalityType":"enfp","loveTypeCategory":"CONFUSION_TYPE"}
                                    """))
                    .andExpect(status().isOk());

            // when - loveTypeCategory만 수정 (personalityType 미포함)
            mockMvc.perform(patch("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"loveTypeCategory":"STABLE_TYPE"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.personalityType").value("ENFP"))
                    .andExpect(jsonPath("$.data.loveTypeCategory").value("STABLE_TYPE"));
        }

        @Test
        @DisplayName("상대 프로필 조회 실패 - 미등록 상태")
        void 파트너_멤버_정보_조회_실패_커플이_아닌_경우() throws Exception {
            // when & then
            mockMvc.perform(get("/members/partner")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_PARTNER_PROFILE.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_PARTNER_PROFILE.getCode()));
        }

        @Test
        @DisplayName("상대 프로필 중복 등록 실패")
        void 파트너_멤버_정보_조회_실패_탈퇴한_멤버인_경우() throws Exception {
            // given
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("personalityType", "ENFP"))))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("personalityType", "INTJ"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(PARTNER_PROFILE_ALREADY_EXISTS.getMessage()))
                    .andExpect(jsonPath("code").value(PARTNER_PROFILE_ALREADY_EXISTS.getCode()));
        }

        @Test
        @DisplayName("멤버 정보 수정 시 MBTI와 애착 유형 반영")
        void 멤버_정보_수정_시_MBTI와_애착유형_반영() throws Exception {
            mockMvc.perform(patch("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"relationshipStatus":"IN_RELATIONSHIP","personalityType":"intj","loveTypeCategory":"STABLE_TYPE"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.relationshipStatus").value(RelationshipStatus.IN_RELATIONSHIP.name()))
                    .andExpect(jsonPath("$.data.personalityType").value("INTJ"))
                    .andExpect(jsonPath("$.data.loveTypeCategory").value(LoveTypeCategory.STABLE_TYPE.name()));

            em.flush();
            em.clear();

            MemberEntity updatedMember = em.find(MemberEntity.class, member.getId());
            Assertions.assertThat(updatedMember.getPersonalityType()).isEqualTo("INTJ");
            Assertions.assertThat(updatedMember.getLoveTypeCategory()).isEqualTo(LoveTypeCategory.STABLE_TYPE);
        }

        @Test
        @DisplayName("초대코드 조회 성공")
        void 초대코드_조회_성공() throws Exception {
            // when
            MvcResult mvcResult = mockMvc.perform(get("/members/invite-code")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            String coupleCode = JsonPath.read(responseContent, "$.data.coupleCode");

            Assertions.assertThat(coupleCode).isEqualTo(member.getInviteCodeEntityValue().getValue());
        }

        @Test
        @DisplayName("초대코드 조회 실패 - 탈퇴한 멤버인 경우")
        void 초대코드_조회_실패_탈퇴한_멤버인_경우() throws Exception {
            // given
            mockMvc.perform(delete("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(get("/members/invite-code")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_MEMBER.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_MEMBER.getCode()));
        }
    }

    @Nested
    @DisplayName("멤버 정보 수정 검증 (기존 정책)")
    class MemberInfoUpdateFeature {
        @Test
        @DisplayName("멤버 정보 수정 성공")
        void 멤버_정보_수정_성공() throws Exception {
            // given
            String newNickname = "newName";
            // when
            MvcResult mvcResult = mockMvc.perform(patch("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createUpdateMemberRequestDto(newNickname)
                            )))
                    .andExpect(status().isOk())
                    .andReturn();

            // then
            String responseContent = mvcResult.getResponse().getContentAsString();
            String response = JsonPath.read(responseContent, "$.data.nickname");

            MemberEntity savedMember = em.createQuery("SELECT m FROM MemberEntity m WHERE m.email = :email", MemberEntity.class)
                    .setParameter("email", member.getEmail())
                    .getSingleResult();

            Assertions.assertThat(response).isEqualTo(newNickname);
            Assertions.assertThat(savedMember.getNickname()).isEqualTo(newNickname);
        }

        @Test
        @DisplayName("멤버 정보 수정 실패 - 탈퇴한 멤버인 경우")
        void 멤버_정보_수정_실패_탈퇴한_멤버인_경우() throws Exception {
            // given
            mockMvc.perform(delete("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(patch("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createUpdateMemberRequestDto("newName")
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_MEMBER.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_MEMBER.getCode()));
        }

        @Test
        @DisplayName("멤버 정보 수정 실패 - 특수 기호가 포함된 경우")
        void 멤버_정보_수정_실패_닉네임_규격에_맞지_않는_경우() throws Exception {
            // given
            String invalidNickname = "invalid!@#";

            // when & then
            mockMvc.perform(patch("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createUpdateMemberRequestDto(invalidNickname)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("멤버 정보 수정 실패 - 길이가 초과된 경우")
        void 멤버_정보_수정_실패_닉네임_길이가_초과된_경우() throws Exception {
            // given
            String invalidNickname = "invalid1234"; // 11자

            // when & then
            mockMvc.perform(patch("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createUpdateMemberRequestDto(invalidNickname)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("디데이 수정 성공")
        void 디데이_수정_성공() throws Exception {
            // V2 정책에서는 솔로 사용자는 디데이 수정이 불가능하므로 이 테스트는 커플 사용자로 변경
            // given - 커플 사용자 생성
            MemberEntity partner = createAndSavePartner();
            
            // 커플 연동
            mockMvc.perform(post("/couples")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    CoupleRequestDtoFactory.createCoupleLinkRequestDto(partner.getInviteCodeEntityValue().getValue())
                            )))
                    .andExpect(status().isOk());

            // when
            mockMvc.perform(patch("/members/start-love-date")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createUpdateStartLoveDateRequestDto(newDday)
                            )))
                    .andExpect(status().isOk());

            // then - V2 정책에서는 개인의 startLoveDate는 수정되지 않고, 커플의 startLoveDate만 수정됨
            em.flush();
            em.clear();
            MemberEntity savedMember = em.createQuery("SELECT m FROM MemberEntity m WHERE m.email = :email", MemberEntity.class)
                    .setParameter("email", member.getEmail())
                    .getSingleResult();

            // V2 정책에서는 개인의 startLoveDate는 null이어야 함
            Assertions.assertThat(savedMember.getStartLoveDate()).isNull();
        }

        @Test
        @DisplayName("커플 멤버인 경우 디데이 수정 성공")
        void 커플_멤버인_경우_디데이_수정_성공() throws Exception {
            // given
            MemberEntity partner = createAndSavePartner();
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
            mockMvc.perform(patch("/members/start-love-date")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createUpdateStartLoveDateRequestDto(newDday)
                            )))
                    .andExpect(status().isOk());

            // then - V2 정책에서는 개인의 startLoveDate는 수정되지 않고, 커플의 startLoveDate만 수정됨
            MemberEntity savedMember = em.createQuery("SELECT m FROM MemberEntity m WHERE m.email = :email", MemberEntity.class)
                    .setParameter("email", member.getEmail())
                    .getSingleResult();
            // V2 정책에서는 개인의 startLoveDate는 null이어야 함
            Assertions.assertThat(savedMember.getStartLoveDate()).isNull();

            // 커플의 디데이만 수정되어야 함
            CoupleEntity couple = em.createQuery("SELECT c FROM CoupleEntity c WHERE c.id = :coupleId", CoupleEntity.class)
                    .setParameter("coupleId", Long.valueOf(coupleId))
                    .getSingleResult();
            Assertions.assertThat(couple.getStartLoveDate()).isEqualTo(newDday);
        }

        @Test
        @DisplayName("디데이 수정 실패 - 탈퇴한 멤버인 경우")
        void 디데이_수정_실패_탈퇴한_멤버인_경우() throws Exception {
            // given
            mockMvc.perform(delete("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // when & then
            mockMvc.perform(patch("/members/start-love-date")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createUpdateStartLoveDateRequestDto(newDday)
                            )))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("message").value(ErrorCode.NOT_COUPLE_MEMBER.getMessage()))
                    .andExpect(jsonPath("code").value(ErrorCode.NOT_COUPLE_MEMBER.getCode()));
        }

        @Test
        @DisplayName("디데이 수정 실패 - 디데이가 오늘보다 이후인 경우")
        void 디데이_수정_실패_디데이가_오늘보다_이후인_경우() throws Exception {
            // given
            LocalDate futureDday = LocalDate.now().plusDays(1);
            // when & then
            mockMvc.perform(patch("/members/start-love-date")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createUpdateStartLoveDateRequestDto(futureDday)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("애착 유형 등록 성공 - 회피형")
        void 애착_유형_등록_성공_회피형() throws Exception {
            // given
            int[] scores = {5, 1, 1, 5, 1, 1, 1, 5, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 5, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 1, 1, 1, 5, 1};

            // when
            mockMvc.perform(post("/members/love-type")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk());

            // then
            MemberEntity updatedMember = em.createQuery("SELECT m FROM MemberEntity m WHERE m.email = :email", MemberEntity.class)
                    .setParameter("email", member.getEmail())
                    .getSingleResult();
            Assertions.assertThat(updatedMember.getLoveTypeCategory()).isEqualTo(LoveTypeCategory.AVOIDANCE_TYPE);
            Assertions.assertThat(updatedMember.getAvoidanceRate()).isEqualTo(5.00f);
            Assertions.assertThat(updatedMember.getAnxietyRate()).isEqualTo(1.00f);
        }

        @Test
        @DisplayName("애착 유형 등록 성공 - 불안형")
        void 애착_유형_등록_성공_불안형() throws Exception {
            // given
            int[] scores = {1, 5, 5, 1, 5, 5, 5, 1, 5, 5, 1, 5, 5, 5, 5, 1, 5, 5, 1, 5, 5, 5, 1, 5, 5, 5, 5, 1, 5, 5, 5, 5, 5, 5, 1, 5};

            // when
            mockMvc.perform(post("/members/love-type")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk());

            // then
            MemberEntity updatedMember = em.createQuery("SELECT m FROM MemberEntity m WHERE m.email = :email", MemberEntity.class)
                    .setParameter("email", member.getEmail())
                    .getSingleResult();
            Assertions.assertThat(updatedMember.getLoveTypeCategory()).isEqualTo(LoveTypeCategory.ANXIETY_TYPE);
            Assertions.assertThat(updatedMember.getAvoidanceRate()).isEqualTo(1.00f);
            Assertions.assertThat(updatedMember.getAnxietyRate()).isEqualTo(5.00f);
        }

        @Test
        @DisplayName("애착 유형 등록 성공 - 혼란형")
        void 애착_유형_등록_성공_혼란형() throws Exception {
            // given
            int[] scores = {5, 5, 1, 5, 1, 5, 1, 1, 1, 5, 5, 5, 5, 5, 5, 5, 5, 1, 5, 5, 5, 5, 1, 5, 5, 5, 1, 5, 1, 1, 5, 1, 1, 1, 5, 5};

            // when
            mockMvc.perform(post("/members/love-type")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk());

            // then
            MemberEntity updatedMember = em.createQuery("SELECT m FROM MemberEntity m WHERE m.email = :email", MemberEntity.class)
                    .setParameter("email", member.getEmail())
                    .getSingleResult();
            Assertions.assertThat(updatedMember.getLoveTypeCategory()).isEqualTo(LoveTypeCategory.CONFUSION_TYPE);
            Assertions.assertThat(updatedMember.getAvoidanceRate()).isEqualTo(5.00f);
            Assertions.assertThat(updatedMember.getAnxietyRate()).isEqualTo(5.00f);
        }

        @Test
        @DisplayName("애착 유형 등록 성공 - 안정형")
        void 애착_유형_등록_성공_안정형() throws Exception {
            // given
            int[] scores = {1, 1, 5, 1, 5, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1};

            // when
            mockMvc.perform(post("/members/love-type")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isOk());

            // then
            MemberEntity updatedMember = em.createQuery("SELECT m FROM MemberEntity m WHERE m.email = :email", MemberEntity.class)
                    .setParameter("email", member.getEmail())
                    .getSingleResult();
            Assertions.assertThat(updatedMember.getLoveTypeCategory()).isEqualTo(LoveTypeCategory.STABLE_TYPE);
            Assertions.assertThat(updatedMember.getAvoidanceRate()).isEqualTo(1.00f);
            Assertions.assertThat(updatedMember.getAnxietyRate()).isEqualTo(1.00f);
        }

        @Test
        @DisplayName("애착 유형 등록 실패 - 탈퇴한 멤버인 경우")
        void 애착_유형_등록_실패_탈퇴한_멤버인_경우() throws Exception {
            // given
            mockMvc.perform(delete("/members")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            int[] scores = {1, 1, 5, 1, 5, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1};
            // when & then
            mockMvc.perform(post("/members/love-type")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_MEMBER.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_MEMBER.getCode()));
        }

        @Test
        @DisplayName("애착 유형 등록 실패 - 점수가 0점인 경우")
        void 애착_유형_등록_실패_점수가_0점인_경우() throws Exception {
            // given
            int[] scores = {1, 1, 5, 1, 0, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1};
            // when & then
            mockMvc.perform(post("/members/love-type")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("애착 유형 등록 실패 - 점수가 6점인 경우")
        void 애착_유형_등록_실패_점수가_6점인_경우() throws Exception {
            // given
            int[] scores = {1, 1, 5, 1, 6, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1};
            // when & then
            mockMvc.perform(post("/members/love-type")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(BAD_REQUEST.getMessage()))
                    .andExpect(jsonPath("code").value(BAD_REQUEST.getCode()));
        }

        @Test
        @DisplayName("애착 유형 등록 실패 - 존재하지 않는 질문인 경우")
        void 애착_유형_등록_실패_존재하지_않는_질문인_경우() throws Exception {
            // given
            // 36개의 질문에 대한 점수 배열이 필요하지만, 37번에 대한 답변을 했다고 가정
            int[] scores = {1, 1, 5, 1, 1, 1, 5, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 5, 1, 1, 1, 1, 5, 1, 1, 1, 5, 1, 5, 5, 1, 5, 5, 5, 1, 1, 4};
            // when & then
            mockMvc.perform(post("/members/love-type")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    MemberRequestDtoFactory.createRegisterLoveTypeRequestDto(scores)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("message").value(NO_SUCH_LOVE_TYPE_QUESTION.getMessage()))
                    .andExpect(jsonPath("code").value(NO_SUCH_LOVE_TYPE_QUESTION.getCode()));
        }
    }


    private MemberEntity createAndSavePartner() {
        MemberEntity partner = MemberEntity.builder()
                .provider(Provider.KAKAO)
                .providerId("partnerProviderId")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .nickname("pnickname")
                .loveTypeCategory(LoveTypeCategory.ANXIETY_TYPE)
                .anxietyRate(2.77f)
                .avoidanceRate(1.66f)
                .email("testEmail2@test.com")
                .inviteCodeEntityValue(InviteCodeEntityValue.of("invite2"))
                .build();

        em.persist(partner);
        em.flush();
        return partner;
    }

    @Nested
    @DisplayName("상대 프로필 등록/수정 API 테스트 (POST/PATCH /members/partners)")
    class PartnerProfileTest {

        @Test
        @DisplayName("MBTI만 전송하면 loveTypeCategory는 null로 저장된다")
        void createPartnerProfile_mbtiOnly_loveTypeCategoryIsNull() throws Exception {
            // given
            Map<String, Object> requestDto = Map.of("personalityType", "INFP");

            // when & then
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.personalityType").value("INFP"))
                    .andExpect(jsonPath("$.data.loveTypeCategory").doesNotExist());

            em.flush();
            em.clear();

            MemberEntity savedMember = em.find(MemberEntity.class, member.getId());
            assertThat(savedMember.getOtherPersonalityType()).isEqualTo("INFP");
            assertThat(savedMember.getPartnerLoveTypeCategory()).isNull();
        }

        @Test
        @DisplayName("MBTI와 loveTypeCategory 모두 전송하면 두 필드 모두 저장된다")
        void createPartnerProfile_bothFields_allSaved() throws Exception {
            // given
            Map<String, Object> requestDto = Map.of(
                    "personalityType", "INFP",
                    "loveTypeCategory", "STABLE_TYPE"
            );

            // when & then
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.personalityType").value("INFP"))
                    .andExpect(jsonPath("$.data.loveTypeCategory").value("STABLE_TYPE"));

            em.flush();
            em.clear();

            MemberEntity savedMember = em.find(MemberEntity.class, member.getId());
            assertThat(savedMember.getOtherPersonalityType()).isEqualTo("INFP");
            assertThat(savedMember.getPartnerLoveTypeCategory()).isEqualTo(PartnerLoveTypeCategory.STABLE_TYPE);
        }

        @Test
        @DisplayName("UNKNOWN을 명시적으로 전송하면 UNKNOWN으로 저장된다")
        void createPartnerProfile_unknownExplicit_savedAsUnknown() throws Exception {
            // given
            Map<String, Object> requestDto = Map.of(
                    "personalityType", "INFP",
                    "loveTypeCategory", "UNKNOWN"
            );

            // when & then
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.loveTypeCategory").value("UNKNOWN"));

            em.flush();
            em.clear();

            MemberEntity savedMember = em.find(MemberEntity.class, member.getId());
            assertThat(savedMember.getPartnerLoveTypeCategory()).isEqualTo(PartnerLoveTypeCategory.UNKNOWN);
        }

        @Test
        @DisplayName("MBTI 없이 loveTypeCategory만 전송하면 400 오류가 발생한다")
        void createPartnerProfile_missingMbti_returns400() throws Exception {
            // given
            Map<String, Object> requestDto = Map.of("loveTypeCategory", "STABLE_TYPE");

            // when & then
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("MBTI만 등록한 후 PATCH로 loveTypeCategory를 설정할 수 있다")
        void updatePartnerProfile_addLoveTypeAfterMbtiOnly() throws Exception {
            // given - MBTI만 먼저 등록
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("personalityType", "INFP"))))
                    .andExpect(status().isOk());

            // when - loveTypeCategory만 PATCH
            mockMvc.perform(patch("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("loveTypeCategory", "STABLE_TYPE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.personalityType").value("INFP"))
                    .andExpect(jsonPath("$.data.loveTypeCategory").value("STABLE_TYPE"));

            em.flush();
            em.clear();

            MemberEntity savedMember = em.find(MemberEntity.class, member.getId());
            assertThat(savedMember.getOtherPersonalityType()).isEqualTo("INFP");
            assertThat(savedMember.getPartnerLoveTypeCategory()).isEqualTo(PartnerLoveTypeCategory.STABLE_TYPE);
        }

        @Test
        @DisplayName("PATCH에서 loveTypeCategory를 생략하면 기존 값이 유지된다")
        void updatePartnerProfile_omitLoveType_preservesExistingValue() throws Exception {
            // given - MBTI + loveTypeCategory 모두 등록
            mockMvc.perform(post("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "personalityType", "INFP",
                                    "loveTypeCategory", "STABLE_TYPE"
                            ))))
                    .andExpect(status().isOk());

            // when - personalityType만 PATCH (loveTypeCategory 생략)
            mockMvc.perform(patch("/members/partners")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("personalityType", "INTJ"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.personalityType").value("INTJ"))
                    .andExpect(jsonPath("$.data.loveTypeCategory").value("STABLE_TYPE"));
        }
    }

    private CoupleQuestionEntity createAndSaveCoupleQuestion(QuestionEntity questionEntity, Integer coupleId) {
        CoupleQuestionEntity coupleQuestion = CoupleQuestionEntity.builder()
                .question(questionEntity)
                .coupleEntityId(CoupleEntityId.of(Long.valueOf(coupleId)))
                .coupleQuestionState(CoupleQuestionState.OUTDATED)
                .bothAnsweredAt(LocalDateTime.now())
                .build();
        em.persist(coupleQuestion);
        em.flush();

        return coupleQuestion;
    }

    private ChatRoomEntity createAndSaveChatRoom() {
        ChatRoomEntity chatRoom = ChatRoomEntity.builder()
                .memberEntityId(MemberEntityId.of(member.getId()))
                .chatRoomState(ChatRoomState.ALIVE)
                .level(1)
                .lastMessageSentTime(LocalDateTime.now())
                .build();

        em.persist(chatRoom);
        em.flush();
        return chatRoom;
    }

}
