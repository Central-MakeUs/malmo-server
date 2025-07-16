package makeus.cmc.malmo.domain.model.member;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.id.LoveTypeId;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class Member {
    private Long id;
    private Provider provider;
    private String providerId;
    private MemberRole memberRole;
    private MemberState memberState;
    private boolean isAlarmOn;
    private String firebaseToken;
    private String refreshToken;
    private LoveTypeId loveTypeId;
    private float avoidanceRate;
    private float anxietyRate;
    private String nickname;
    private String email;
    private InviteCodeValue inviteCode;
    private LocalDate startLoveDate;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    
    public static Member createMember(Provider provider, String providerId, MemberRole memberRole, MemberState memberState, String email, InviteCodeValue inviteCode) {
        return Member.builder()
                .provider(provider)
                .providerId(providerId)
                .memberRole(memberRole)
                .memberState(memberState)
                .email(email)
                .inviteCode(inviteCode)
                .build();
    }

    public static Member from(
            Long id,
            Provider provider,
            String providerId,
            MemberRole memberRole,
            MemberState memberState,
            boolean isAlarmOn,
            String firebaseToken,
            String refreshToken,
            LoveTypeId loveTypeId,
            float avoidanceRate,
            float anxietyRate,
            String nickname,
            String email,
            InviteCodeValue inviteCode,
            LocalDate startLoveDate,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt,
            LocalDateTime deletedAt
    ) {
        return Member.builder()
                .id(id)
                .provider(provider)
                .providerId(providerId)
                .memberRole(memberRole)
                .memberState(memberState)
                .isAlarmOn(isAlarmOn)
                .firebaseToken(firebaseToken)
                .refreshToken(refreshToken)
                .loveTypeId(loveTypeId)
                .avoidanceRate(avoidanceRate)
                .anxietyRate(anxietyRate)
                .nickname(nickname)
                .email(email)
                .inviteCode(inviteCode)
                .startLoveDate(startLoveDate)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }


    public void signUp(String nickname, LocalDate startLoveDate) {
        this.nickname = nickname;
        this.startLoveDate = startLoveDate;
        this.memberState = MemberState.ALIVE;
    }

    public void updateMemberProfile(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
    }

    public void updateLoveTypeId(LoveTypeId loveTypeId, float avoidanceRate, float anxietyRate) {
        this.loveTypeId = loveTypeId;
        this.avoidanceRate = avoidanceRate / 18.0f;
        this.anxietyRate = anxietyRate / 18.0f;
    }

    public void refreshMemberToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}