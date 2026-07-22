package makeus.cmc.malmo.application.service.member;

import makeus.cmc.malmo.application.helper.member.MemberCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.port.in.member.WebSignInUseCase;
import makeus.cmc.malmo.application.port.out.member.GenerateWebTokenPort;
import makeus.cmc.malmo.application.port.out.member.WebLoginTicketPort;
import makeus.cmc.malmo.application.port.out.member.WebOAuthProviderPort;
import makeus.cmc.malmo.application.port.out.member.WebOAuthStatePort;
import makeus.cmc.malmo.application.port.out.member.WebSessionPort;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.InviteCodeDomainService;
import makeus.cmc.malmo.domain.service.MemberDomainService;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSignInServiceTest {

    @Mock
    private MemberQueryHelper memberQueryHelper;
    @Mock
    private MemberCommandHelper memberCommandHelper;
    @Mock
    private MemberDomainService memberDomainService;
    @Mock
    private InviteCodeDomainService inviteCodeDomainService;
    @Mock
    private WebOAuthStatePort statePort;
    @Mock
    private WebLoginTicketPort ticketPort;
    @Mock
    private WebSessionPort sessionPort;
    @Mock
    private GenerateWebTokenPort tokenPort;
    @Mock
    private WebOAuthProviderPort kakaoProvider;

    private WebSignInService service;

    @BeforeEach
    void setUp() {
        service = new WebSignInService(
                memberQueryHelper,
                memberCommandHelper,
                memberDomainService,
                inviteCodeDomainService,
                statePort,
                ticketPort,
                sessionPort,
                tokenPort,
                kakaoProvider,
                List.of("https://web.malmo.example"),
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void existingMemberWebLoginDoesNotOverwriteMobileMemberToken() {
        Member existingMember = Member.from(
                1L, Provider.KAKAO, "provider-id", MemberRole.MEMBER, MemberState.ALIVE,
                false, null, "mobile-refresh-token", null, 0, 0,
                "nickname", "old@example.com", null, null, null, null,
                null, null, null, null, null, null, null, null
        );
        WebOAuthStatePort.State state = new WebOAuthStatePort.State(
                Provider.KAKAO, "state", "nonce", "verifier",
                "https://web.malmo.example/auth/callback", null
        );
        given(statePort.consume("state")).willReturn(state);
        given(kakaoProvider.exchange(any())).willReturn(
                new WebOAuthProviderPort.Identity("provider-id", "new@example.com")
        );
        given(memberQueryHelper.getMemberByProviderId(Provider.KAKAO, "provider-id"))
                .willReturn(Optional.of(existingMember));
        given(ticketPort.issue(any())).willReturn("ticket");

        URI redirect = service.completeAuthorization("code", "state");

        assertThat(redirect).hasToString("https://web.malmo.example/auth/callback?ticket=ticket");
        assertThat(existingMember.getRefreshToken()).isEqualTo("mobile-refresh-token");
        assertThat(existingMember.getEmail()).isEqualTo("old@example.com");
        verify(memberCommandHelper, never()).saveMember(existingMember);
    }

    @Test
    void exchangeTicketCreatesOnlyWebSession() {
        Member member = Member.from(
                1L, Provider.KAKAO, "provider-id", MemberRole.MEMBER, MemberState.ALIVE,
                false, null, "mobile-refresh-token", null, 0, 0,
                "nickname", "member@example.com", null, null, null, null,
                null, null, null, null, null, null, null, null
        );
        given(ticketPort.consume("ticket")).willReturn(new WebLoginTicketPort.Ticket(1L, "ALIVE"));
        given(memberQueryHelper.getMemberByIdOrThrow(any())).willReturn(member);
        given(tokenPort.generateAccessToken(1L, MemberRole.MEMBER)).willReturn("web-access-token");
        given(tokenPort.generateRefreshToken()).willReturn("web-refresh-token");
        given(tokenPort.hashRefreshToken("web-refresh-token")).willReturn("refresh-hash");

        WebSignInUseCase.SignInResponse response = service.exchangeTicket("ticket");

        assertThat(response.accessToken()).isEqualTo("web-access-token");
        assertThat(response.refreshToken()).isEqualTo("web-refresh-token");
        assertThat(member.getRefreshToken()).isEqualTo("mobile-refresh-token");
        verify(sessionPort).create(1L, "refresh-hash", Instant.parse("2026-08-19T00:00:00Z"));
        verify(memberCommandHelper, never()).saveMember(member);
    }
}
