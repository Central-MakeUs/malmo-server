package makeus.cmc.malmo.integration_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import makeus.cmc.malmo.adaptor.out.oauth.AppleRefreshTokenAdapter;
import makeus.cmc.malmo.adaptor.out.oidc.AppleOidcAdapter;
import makeus.cmc.malmo.adaptor.out.oidc.KakaoOidcAdapter;
import makeus.cmc.malmo.adaptor.out.oidc.KakaoRestApiAdaptor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.integration_test.dto_factory.SignInRequestDtoFactory;
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

import static makeus.cmc.malmo.adaptor.in.exception.ErrorCode.INVALID_REFRESH_TOKEN;
import static makeus.cmc.malmo.adaptor.in.exception.ErrorCode.NO_SUCH_MEMBER;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class SignInIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager em;

    @MockBean
    private AppleOidcAdapter appleOidcAdapter;

    @MockBean
    private KakaoOidcAdapter kakaoOidcAdapter;

    @MockBean
    private KakaoRestApiAdaptor kakaoRestApiAdaptor;

    @MockBean
    private AppleRefreshTokenAdapter appleRefreshTokenAdapter;

    private String accessToken;

    private MemberEntity kakaoMember;

    private MemberEntity appleMember;

    @BeforeEach
    void setup() {
        kakaoMember = MemberEntity.builder()
                .provider(Provider.KAKAO)
                .providerId("kakao-provider-id")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .email("testEmail@test.com")
                .nickname("nickname")
                .startLoveDate(LocalDate.of(2023, 10, 1))
                .inviteCodeEntityValue(InviteCodeEntityValue.of("testInviteCode1"))
                .build();

        appleMember = MemberEntity.builder()
                .provider(Provider.APPLE)
                .providerId("apple-provider-id")
                .memberRole(MemberRole.MEMBER)
                .memberState(MemberState.ALIVE)
                .email("testEmail@test.com")
                .nickname("nickname")
                .startLoveDate(LocalDate.of(2023, 10, 1))
                .inviteCodeEntityValue(InviteCodeEntityValue.of("testInviteCode2"))
                .build();

        em.persist(kakaoMember);
        em.persist(appleMember);
        em.flush();
    }

    @Nested
    @DisplayName("카카오 소셜 로그인 테스트")
    class KakaoSignInTest {

        @Test
        @DisplayName("등록된 사용자의 경우 카카오 소셜 로그인에 성공한다")
        void 등록된_사용자의_경우_카카오_소셜_로그인에_성공한다() throws Exception {
            // given
            String idToken = "valid-id-token";
            String accessToken = "valid-access-token";
            given(kakaoOidcAdapter.validateToken(idToken)).willReturn(kakaoMember.getProviderId());

            // when & then
            mockMvc.perform(post("/login/kakao")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createKakaoLoginRequestDto(idToken, accessToken)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.memberState").value("ALIVE"))
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"));
        }

        @Test
        @DisplayName("등록되지 않은 사용자의 경우 카카오 소셜 로그인에 성공한다")
        void 등록되지_않은_사용자의_경우_카카오_소셜_로그인에_성공한다() throws Exception {
            // given
            String providerId = "guest-provider-id";
            String idToken = "valid-id-token";
            String accessToken = "valid-access-token";
            String email = "guest@email.com";
            given(kakaoOidcAdapter.validateToken(idToken)).willReturn(providerId);
            given(kakaoRestApiAdaptor.fetchMemberEmailFromKakao(accessToken)).willReturn(email);

            // when & then
            mockMvc.perform(post("/login/kakao")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createKakaoLoginRequestDto(idToken, accessToken)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.memberState").value("BEFORE_ONBOARDING"))
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"));

            MemberEntity memberEntity = em.createQuery(
                            "SELECT m FROM MemberEntity m WHERE m.provider = :provider AND m.providerId = :providerId",
                            MemberEntity.class)
                    .setParameter("provider", Provider.KAKAO)
                    .setParameter("providerId", providerId)
                    .getSingleResult();

            Assertions.assertThat(memberEntity.getProvider()).isEqualTo(Provider.KAKAO);
            Assertions.assertThat(memberEntity.getProviderId()).isEqualTo(providerId);
            Assertions.assertThat(memberEntity.getEmail()).isEqualTo(email);
            Assertions.assertThat(memberEntity.getMemberState()).isEqualTo(MemberState.BEFORE_ONBOARDING);
        }

        @Test
        @DisplayName("카카오 소셜 로그인 탈퇴한 멤버의 경우 사용자 정보 복구")
        void 카카오_소셜_로그인_탈퇴한_멤버의_경우_사용자_새로_가입() throws Exception {
            // given
            String idToken = "valid-id-token";
            String accessToken = "valid-access-token";
            String originalProviderId = "deleted-kakao-provider-id";
            given(kakaoOidcAdapter.validateToken(idToken)).willReturn(originalProviderId);
            given(kakaoRestApiAdaptor.fetchMemberEmailFromKakao(accessToken)).willReturn("testEmail@test.com");

            MemberEntity deletedMember = MemberEntity.builder()
                    .provider(Provider.KAKAO)
                    .providerId(originalProviderId + "_deleted_member_test")
                    .memberRole(MemberRole.MEMBER)
                    .memberState(MemberState.DELETED)
                    .email("testEmail@test.com")
                    .inviteCodeEntityValue(InviteCodeEntityValue.of("deletedInviteCode"))
                    .build();
            em.persist(deletedMember);
            em.flush();

            // when & then
            mockMvc.perform(post("/login/kakao")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createKakaoLoginRequestDto(idToken, accessToken)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.memberState").value("BEFORE_ONBOARDING"))
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"));

            MemberEntity newMember = em.createQuery(
                            "SELECT m FROM MemberEntity m WHERE m.provider = :provider AND m.providerId = :providerId",
                            MemberEntity.class)
                    .setParameter("provider", Provider.KAKAO)
                    .setParameter("providerId", originalProviderId)
                    .getSingleResult();

            Assertions.assertThat(newMember.getMemberState()).isEqualTo(MemberState.BEFORE_ONBOARDING);
            Assertions.assertThat(newMember.getDeletedAt()).isNull();
            Assertions.assertThat(newMember.getId()).isNotEqualTo(deletedMember.getId());
        }
    }

    @Nested
    @DisplayName("애플 소셜 로그인 테스트")
    class AppleSignInTest {

        @Test
        @DisplayName("등록된 사용자의 경우 애플 소셜 로그인에 성공한다")
        void 등록된_사용자의_경우_애플_소셜_로그인에_성공한다() throws Exception {
            // given
            String idToken = "valid-id-token";
            String authorizationCode = "valid-authorization-code";
            given(appleOidcAdapter.validateToken(idToken)).willReturn(appleMember.getProviderId());

            // when & then
            mockMvc.perform(post("/login/apple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createAppleLoginRequestDto(idToken, authorizationCode)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.memberState").value("ALIVE"))
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"));
        }

        @Test
        @DisplayName("등록되지 않은 사용자의 경우 애플 소셜 로그인에 성공한다")
        void 등록되지_않은_사용자의_경우_애플_소셜_로그인에_성공한다() throws Exception {
            // given
            String providerId = "guest-provider-id";
            String idToken = "valid-id-token";
            String email = "guest@email.com";
            String authorizationCode = "valid-authorization-code";
            given(appleOidcAdapter.validateToken(idToken)).willReturn(providerId);
            given(appleOidcAdapter.extractEmailFromIdToken(idToken)).willReturn(email);
            given(appleRefreshTokenAdapter.getAppleRefreshToken(authorizationCode)).willReturn("valid-refresh-token");

            // when & then
            mockMvc.perform(post("/login/apple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createAppleLoginRequestDto(idToken, authorizationCode)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.memberState").value("BEFORE_ONBOARDING"))
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"));

            MemberEntity memberEntity = em.createQuery(
                            "SELECT m FROM MemberEntity m WHERE m.provider = :provider AND m.providerId = :providerId",
                            MemberEntity.class)
                    .setParameter("provider", Provider.APPLE)
                    .setParameter("providerId", providerId)
                    .getSingleResult();

            Assertions.assertThat(memberEntity.getProvider()).isEqualTo(Provider.APPLE);
            Assertions.assertThat(memberEntity.getProviderId()).isEqualTo(providerId);
            Assertions.assertThat(memberEntity.getEmail()).isEqualTo(email);
            Assertions.assertThat(memberEntity.getMemberState()).isEqualTo(MemberState.BEFORE_ONBOARDING);
            Assertions.assertThat(memberEntity.getOauthToken()).isEqualTo("valid-refresh-token");
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class RefreshTokenTest {

        @Test
        @DisplayName("리프레시 토큰을 통한 액세스 토큰 재발급에 성공한다")
        void 리프레시_토큰을_통한_액세스_토큰_재발급에_성공한다() throws Exception {
            // given
            String idToken = "valid-id-token";
            String authorizationCode = "valid-authorization-code";
            given(appleOidcAdapter.validateToken(idToken)).willReturn(appleMember.getProviderId());

            MvcResult loginResult = mockMvc.perform(post("/login/apple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createAppleLoginRequestDto(idToken, authorizationCode)
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseContent = loginResult.getResponse().getContentAsString();
            String refreshToken = objectMapper.readTree(responseContent).get("data").get("refreshToken").asText();

            // when & then
            MvcResult refreshResult = mockMvc.perform(post("/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createRefreshRequestDto(refreshToken)
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"))
                    .andReturn();
            String refreshResponseContent = refreshResult.getResponse().getContentAsString();
            String rotatedRefreshToken = objectMapper.readTree(refreshResponseContent).get("data").get("refreshToken").asText();

            // RTR 방식을 이용하기 때문에 Refresh Token도 변경되어야 함
            MemberEntity savedMember = em.find(MemberEntity.class, appleMember.getId());
            Assertions.assertThat(savedMember.getRefreshToken()).isNotNull();
            Assertions.assertThat(savedMember.getRefreshToken()).isEqualTo(rotatedRefreshToken);
            Assertions.assertThat(savedMember.getRefreshToken()).isNotEqualTo(refreshToken);
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰을 통한 액세스 토큰 재발급에 실패한다")
        void 유효하지_않은_리프레시_토큰을_통한_액세스_토큰_재발급에_실패한다() throws Exception {
            String invalidRefreshToken = "invalid-refresh-token";
            // given
            mockMvc.perform(post("/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createRefreshRequestDto(invalidRefreshToken)
                            )))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(INVALID_REFRESH_TOKEN.getMessage()))
                    .andExpect(jsonPath("$.code").value(INVALID_REFRESH_TOKEN.getCode()));
        }

        @Test
        @DisplayName("탈퇴한 사용자의 리프레시 토큰을 통한 액세스 토큰 재발급에 실패한다")
        void 탈퇴한_사용자의_리프레시_토큰을_통한_액세스_토큰_재발급에_실패한다() throws Exception {
            // given
            String idToken = "valid-id-token";
            String authorizationCode = "valid-authorization-code";
            given(appleOidcAdapter.validateToken(idToken)).willReturn(appleMember.getProviderId());

            MvcResult loginResult = mockMvc.perform(post("/login/apple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createAppleLoginRequestDto(idToken, authorizationCode)
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseContent = loginResult.getResponse().getContentAsString();
            String refreshToken = objectMapper.readTree(responseContent).get("data").get("refreshToken").asText();

            em.createQuery("UPDATE MemberEntity m SET m.memberState = :state, m.deletedAt = :deletedAt WHERE m.id = :memberId")
                    .setParameter("state", MemberState.DELETED)
                    .setParameter("deletedAt", LocalDateTime.now())
                    .setParameter("memberId", appleMember.getId())
                    .executeUpdate();

            // when & then
            mockMvc.perform(post("/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createRefreshRequestDto(refreshToken)
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(NO_SUCH_MEMBER.getMessage()))
                    .andExpect(jsonPath("$.code").value(NO_SUCH_MEMBER.getCode()));
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃에 성공한다")
        void 로그아웃에_성공한다() throws Exception {
            // given
            String idToken = "valid-id-token";
            String accessToken = "valid-access-token";
            given(kakaoOidcAdapter.validateToken(idToken)).willReturn(kakaoMember.getProviderId());
            given(kakaoRestApiAdaptor.fetchMemberEmailFromKakao(accessToken)).willReturn("testEmail@test.com");

            MvcResult mvcResult = mockMvc.perform(post("/login/kakao")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createKakaoLoginRequestDto(idToken, accessToken)
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseContent = mvcResult.getResponse().getContentAsString();
            accessToken = objectMapper.readTree(responseContent).get("data").get("accessToken").asText();

            // when
            mockMvc.perform(post("/logout")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // then
            MemberEntity entity = em.find(MemberEntity.class, kakaoMember.getId());
            Assertions.assertThat(entity.getRefreshToken()).isNull();
        }

        @Test
        @DisplayName("로그아웃 시 회원이 존재하지 않으면 실패한다")
        void 로그아웃_시_탈퇴한_회원이면_실패한다() throws Exception {
            // given
            String idToken = "valid-id-token";
            String accessToken = "valid-access-token";
            given(kakaoOidcAdapter.validateToken(idToken)).willReturn(kakaoMember.getProviderId());
            given(kakaoRestApiAdaptor.fetchMemberEmailFromKakao(accessToken)).willReturn("testEmail@test.com");

            MvcResult mvcResult = mockMvc.perform(post("/login/kakao")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    SignInRequestDtoFactory.createKakaoLoginRequestDto(idToken, accessToken)
                            )))
                    .andExpect(status().isOk())
                    .andReturn();
            String responseContent = mvcResult.getResponse().getContentAsString();
            accessToken = objectMapper.readTree(responseContent).get("data").get("accessToken").asText();

            em.createQuery("UPDATE MemberEntity m SET m.memberState = :state, m.deletedAt = :deletedAt WHERE m.id = :memberId")
                    .setParameter("state", MemberState.DELETED)
                    .setParameter("deletedAt", LocalDateTime.now())
                    .setParameter("memberId", kakaoMember.getId())
                    .executeUpdate();

            mockMvc.perform(post("/logout")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(NO_SUCH_MEMBER.getMessage()))
                    .andExpect(jsonPath("$.code").value(NO_SUCH_MEMBER.getCode()));
        }
    }
}
