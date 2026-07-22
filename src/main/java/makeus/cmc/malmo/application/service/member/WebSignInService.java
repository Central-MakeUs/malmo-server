package makeus.cmc.malmo.application.service.member;

import makeus.cmc.malmo.application.exception.InviteCodeGenerateFailedException;
import makeus.cmc.malmo.application.exception.InvalidWebOAuthRequestException;
import makeus.cmc.malmo.application.helper.member.MemberCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.port.in.member.WebSignInUseCase;
import makeus.cmc.malmo.application.port.out.member.GenerateWebTokenPort;
import makeus.cmc.malmo.application.port.out.member.WebLoginTicketPort;
import makeus.cmc.malmo.application.port.out.member.WebOAuthProviderPort;
import makeus.cmc.malmo.application.port.out.member.WebOAuthStatePort;
import makeus.cmc.malmo.application.port.out.member.WebSessionPort;
import makeus.cmc.malmo.config.WebSocialLoginProperties;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.InviteCodeDomainService;
import makeus.cmc.malmo.domain.service.MemberDomainService;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class WebSignInService implements WebSignInUseCase {

    private static final int MAX_INVITE_CODE_RETRY = 10;
    private static final long DEFAULT_REFRESH_TOKEN_EXPIRATION_SECONDS = 2_592_000;

    private final MemberQueryHelper memberQueryHelper;
    private final MemberCommandHelper memberCommandHelper;
    private final MemberDomainService memberDomainService;
    private final InviteCodeDomainService inviteCodeDomainService;
    private final WebOAuthStatePort statePort;
    private final WebLoginTicketPort ticketPort;
    private final WebSessionPort sessionPort;
    private final GenerateWebTokenPort tokenPort;
    private final WebOAuthProviderPort providerPort;
    private final WebReturnUrlValidator returnUrlValidator;
    private final long refreshTokenExpirationSeconds;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public WebSignInService(
            MemberQueryHelper memberQueryHelper,
            MemberCommandHelper memberCommandHelper,
            MemberDomainService memberDomainService,
            InviteCodeDomainService inviteCodeDomainService,
            WebOAuthStatePort statePort,
            WebLoginTicketPort ticketPort,
            WebSessionPort sessionPort,
            GenerateWebTokenPort tokenPort,
            WebOAuthProviderPort providerPort,
            WebSocialLoginProperties properties,
            Clock clock
    ) {
        this(memberQueryHelper, memberCommandHelper, memberDomainService, inviteCodeDomainService,
                statePort, ticketPort, sessionPort, tokenPort, providerPort,
                properties.getAllowedReturnOrigins(), properties.getRefreshTokenExpirationSeconds(), clock);
    }

    WebSignInService(
            MemberQueryHelper memberQueryHelper,
            MemberCommandHelper memberCommandHelper,
            MemberDomainService memberDomainService,
            InviteCodeDomainService inviteCodeDomainService,
            WebOAuthStatePort statePort,
            WebLoginTicketPort ticketPort,
            WebSessionPort sessionPort,
            GenerateWebTokenPort tokenPort,
            WebOAuthProviderPort providerPort,
            List<String> allowedReturnOrigins,
            Clock clock
    ) {
        this(memberQueryHelper, memberCommandHelper, memberDomainService, inviteCodeDomainService,
                statePort, ticketPort, sessionPort, tokenPort, providerPort,
                allowedReturnOrigins, DEFAULT_REFRESH_TOKEN_EXPIRATION_SECONDS, clock);
    }

    private WebSignInService(
            MemberQueryHelper memberQueryHelper,
            MemberCommandHelper memberCommandHelper,
            MemberDomainService memberDomainService,
            InviteCodeDomainService inviteCodeDomainService,
            WebOAuthStatePort statePort,
            WebLoginTicketPort ticketPort,
            WebSessionPort sessionPort,
            GenerateWebTokenPort tokenPort,
            WebOAuthProviderPort providerPort,
            List<String> allowedReturnOrigins,
            long refreshTokenExpirationSeconds,
            Clock clock
    ) {
        this.memberQueryHelper = memberQueryHelper;
        this.memberCommandHelper = memberCommandHelper;
        this.memberDomainService = memberDomainService;
        this.inviteCodeDomainService = inviteCodeDomainService;
        this.statePort = statePort;
        this.ticketPort = ticketPort;
        this.sessionPort = sessionPort;
        this.tokenPort = tokenPort;
        this.providerPort = providerPort;
        this.returnUrlValidator = new WebReturnUrlValidator(allowedReturnOrigins);
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        this.clock = clock;
    }

    @Override
    public URI startAuthorization(String returnUrl, String deviceId) {
        returnUrlValidator.validate(returnUrl);
        String state = randomUrlSafeValue();
        String nonce = randomUrlSafeValue();
        String codeVerifier = randomUrlSafeValue();
        statePort.save(new WebOAuthStatePort.State(
                Provider.KAKAO, state, nonce, codeVerifier, returnUrl, deviceId
        ));
        return providerPort.authorizationUri(new WebOAuthProviderPort.AuthorizationRequest(
                state, nonce, createCodeChallenge(codeVerifier)
        ));
    }

    @Override
    @Transactional
    public URI completeAuthorization(String code, String stateValue) {
        WebOAuthStatePort.State state = consumeState(stateValue);
        WebOAuthProviderPort.Identity identity = providerPort.exchange(
                new WebOAuthProviderPort.AuthorizationCode(
                        code, state.state(), state.nonce(), state.codeVerifier()
                )
        );
        Member member = memberQueryHelper.getMemberByProviderId(Provider.KAKAO, identity.providerId())
                .orElseGet(() -> createNewMember(identity));

        String ticket = ticketPort.issue(new WebLoginTicketPort.Ticket(
                member.getId(), member.getMemberState().name()
        ));
        return appendQuery(state.returnUrl(), "ticket", ticket);
    }

    @Override
    public URI failAuthorization(String stateValue, String error) {
        WebOAuthStatePort.State state = consumeState(stateValue);
        return appendQuery(state.returnUrl(), "oauthError", sanitizeOAuthError(error));
    }

    @Override
    public SignInResponse exchangeTicket(String ticketValue) {
        WebLoginTicketPort.Ticket ticket = ticketPort.consume(ticketValue);
        Member member = memberQueryHelper.getMemberByIdOrThrow(MemberId.of(ticket.memberId()));
        IssuedWebTokens tokens = issueTokens(member);
        return new SignInResponse(
                ticket.memberState(), "Bearer", tokens.accessToken(), tokens.refreshToken()
        );
    }

    @Override
    public TokenResponse refresh(String currentRefreshToken) {
        String newRefreshToken = tokenPort.generateRefreshToken();
        Instant newExpiry = refreshTokenExpiry();
        Long memberId = sessionPort.rotate(
                tokenPort.hashRefreshToken(currentRefreshToken),
                tokenPort.hashRefreshToken(newRefreshToken),
                newExpiry
        );
        Member member = memberQueryHelper.getMemberByIdOrThrow(MemberId.of(memberId));
        String accessToken = tokenPort.generateAccessToken(member.getId(), member.getMemberRole());
        return new TokenResponse("Bearer", accessToken, newRefreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        sessionPort.revoke(tokenPort.hashRefreshToken(refreshToken));
    }

    private Member createNewMember(WebOAuthProviderPort.Identity identity) {
        Member newMember = memberDomainService.createMember(
                Provider.KAKAO, identity.providerId(), identity.email(), createInviteCode(), null
        );
        return memberCommandHelper.saveMember(newMember);
    }

    private InviteCodeValue createInviteCode() {
        for (int attempt = 0; attempt < MAX_INVITE_CODE_RETRY; attempt++) {
            InviteCodeValue inviteCode = inviteCodeDomainService.generateInviteCode();
            if (memberQueryHelper.isInviteCodeValid(inviteCode)) {
                return inviteCode;
            }
        }
        throw new InviteCodeGenerateFailedException("웹 로그인 회원의 초대 코드 생성에 실패했습니다.");
    }

    private IssuedWebTokens issueTokens(Member member) {
        String accessToken = tokenPort.generateAccessToken(member.getId(), member.getMemberRole());
        String refreshToken = tokenPort.generateRefreshToken();
        sessionPort.create(
                member.getId(), tokenPort.hashRefreshToken(refreshToken), refreshTokenExpiry()
        );
        return new IssuedWebTokens(accessToken, refreshToken);
    }

    private Instant refreshTokenExpiry() {
        return clock.instant().plusSeconds(refreshTokenExpirationSeconds);
    }

    private WebOAuthStatePort.State consumeState(String stateValue) {
        WebOAuthStatePort.State state = statePort.consume(stateValue);
        if (state.provider() != Provider.KAKAO) {
            throw new InvalidWebOAuthRequestException("소셜 로그인 state의 공급자가 일치하지 않습니다.");
        }
        return state;
    }

    private String randomUrlSafeValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String createCodeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    private static URI appendQuery(String returnUrl, String name, String value) {
        return UriComponentsBuilder.fromUriString(returnUrl)
                .queryParam(name, value)
                .build()
                .encode()
                .toUri();
    }

    private static String sanitizeOAuthError(String error) {
        if (error == null || error.isBlank()) {
            return "authorization_failed";
        }
        return error.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private record IssuedWebTokens(String accessToken, String refreshToken) {
    }
}
