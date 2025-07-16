package makeus.cmc.malmo.application.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.application.port.in.SignInUseCase;
import makeus.cmc.malmo.application.port.out.*;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.service.InviteCodeDomainService;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.domain.service.MemberDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SignInService implements SignInUseCase {

    private final MemberDomainService memberDomainService;
    private final InviteCodeDomainService inviteCodeDomainService;
    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final GenerateTokenPort generateTokenPort;
    private final kakaoIdTokenPort kakaoIdTokenPort;
    private final AppleIdTokenPort appleIdTokenPort;

    private final FetchFromOAuthProviderPort fetchFromOAuthProviderPort;

    @Override
    @Transactional
    public SignInResponse signInKakao(SignInKakaoCommand command) {
        // 1. OIDC ID 토큰 검증
        String providerId = kakaoIdTokenPort.validateToken(command.getIdToken());

        // 2. ID 토큰에서 provider와 providerId 추출
        Member member = loadMemberPort.loadMemberByProviderId(Provider.KAKAO, providerId)
                // 3. 없으면 새로 생성 (자동 회원가입)
                .orElseGet(() -> {
                    // 이메일 정보 가져오기
                    String email = fetchFromOAuthProviderPort.fetchMemberEmailFromKakao(command.getAccessToken());
                    InviteCodeValue inviteCodeValue = inviteCodeDomainService.generateUniqueInviteCode();
                    Member newMember = memberDomainService.createMember(Provider.KAKAO, providerId, email, inviteCodeValue);
                    return saveMemberPort.saveMember(newMember);
                });

        // 4. JWT 토큰 발급
        TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());

        // 5. 멤버 정보 갱신 (리프레시 토큰 저장)
        member.refreshMemberToken(tokenInfo.getRefreshToken());
        saveMemberPort.saveMember(member);

        return SignInResponse.builder()
                .memberState(member.getMemberState().name())
                .grantType(tokenInfo.getGrantType())
                .accessToken(tokenInfo.getAccessToken())
                .refreshToken(tokenInfo.getRefreshToken())
                .build();
    }

    @Override
    @Transactional
    public SignInResponse signInApple(SignInAppleCommand command) {
        // 1. OIDC ID 토큰 검증
        String providerId = appleIdTokenPort.validateToken(command.getIdToken());

        // 2. ID 토큰에서 provider와 providerId 추출
        Member member = loadMemberPort.loadMemberByProviderId(Provider.APPLE, providerId)
                // 3. 없으면 새로 생성 (자동 회원가입)
                .orElseGet(() -> {
                    // 이메일 정보 가져오기
                    String email = appleIdTokenPort.extractEmailFromIdToken(command.getIdToken());
                    InviteCodeValue inviteCodeValue = inviteCodeDomainService.generateUniqueInviteCode();
                    Member newMember = memberDomainService.createMember(Provider.APPLE, providerId, email, inviteCodeValue);
                    return saveMemberPort.saveMember(newMember);
                });

        // 4. JWT 토큰 발급
        TokenInfo tokenInfo = generateTokenPort.generateToken(member.getId(), member.getMemberRole());

        // 5. 멤버 정보 갱신 (리프레시 토큰 저장)
        member.refreshMemberToken(tokenInfo.getRefreshToken());
        saveMemberPort.saveMember(member);

        return SignInResponse.builder()
                .memberState(member.getMemberState().name())
                .grantType(tokenInfo.getGrantType())
                .accessToken(tokenInfo.getAccessToken())
                .refreshToken(tokenInfo.getRefreshToken())
                .build();
    }
}
