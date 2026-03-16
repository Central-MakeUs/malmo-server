package makeus.cmc.malmo.domain.model.member;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.id.CoupleId;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.type.EmailForwardingStatus;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;

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
    private LoveTypeCategory loveTypeCategory;
    private float avoidanceRate;
    private float anxietyRate;
    private String nickname;
    private String email;
    private EmailForwardingStatus emailForwardingStatus;
    private InviteCodeValue inviteCode;

    /**
     * @deprecated 개인의 startLoveDate는 더 이상 사용하지 않습니다.
     * 커플의 startLoveDate를 사용하도록 변경되었습니다.
     * Entity 호환성을 위해 필드는 유지됩니다.
     */
    @Deprecated
    private LocalDate startLoveDate;
    private String oauthToken;
    private CoupleId coupleId;

    private RelationshipStatus relationshipStatus;
    private String personalityType;
    private String otherPersonalityType;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;


    public static Member createMember(Provider provider, String providerId, MemberRole memberRole, MemberState memberState, String email, InviteCodeValue inviteCode, String oauthToken) {
        return Member.builder()
                .provider(provider)
                .providerId(providerId)
                .memberRole(memberRole)
                .memberState(memberState)
                .email(email)
                .emailForwardingStatus(EmailForwardingStatus.ENABLED) // 기본값 ENABLED
                .inviteCode(inviteCode)
                .oauthToken(oauthToken)
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
            LoveTypeCategory loveTypeCategory,
            float avoidanceRate,
            float anxietyRate,
            String nickname,
            String email,
            EmailForwardingStatus emailForwardingStatus,
            InviteCodeValue inviteCode,
            LocalDate startLoveDate,
            String oauthToken,
            CoupleId coupleId,
            RelationshipStatus relationshipStatus,
            String personalityType,
            String otherPersonalityType,
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
                .loveTypeCategory(loveTypeCategory)
                .avoidanceRate(avoidanceRate)
                .anxietyRate(anxietyRate)
                .nickname(nickname)
                .email(email)
                .emailForwardingStatus(emailForwardingStatus)
                .inviteCode(inviteCode)
                .startLoveDate(startLoveDate)
                .oauthToken(oauthToken)
                .coupleId(coupleId)
                .relationshipStatus(relationshipStatus)
                .personalityType(personalityType)
                .otherPersonalityType(otherPersonalityType)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }


    /**
     * V1 회원가입 - startLoveDate를 개인 정보로 저장
     * @deprecated v2에서는 signUpV2(String nickname) 사용
     */
    @Deprecated
    public void signUp(String nickname, LocalDate startLoveDate) {
        this.nickname = nickname;
        this.startLoveDate = startLoveDate;
        this.memberState = MemberState.ALIVE;
    }

    public void signUp(String nickname, RelationshipStatus relationshipStatus) {
        this.nickname = nickname;
        this.relationshipStatus = relationshipStatus;
        this.memberState = MemberState.ALIVE;
    }

    public void updateMemberProfile(String nickname, RelationshipStatus relationshipStatus, String personalityType, String otherPersonalityType) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (relationshipStatus != null) {
            this.relationshipStatus = relationshipStatus;
        }
        if (personalityType != null) {
            this.personalityType = personalityType;
        }
        if (otherPersonalityType != null) {
            this.otherPersonalityType = otherPersonalityType;
        }
    }

    public void updateLoveType(LoveTypeCategory loveTypeCategory, float avoidanceRate, float anxietyRate) {
        this.loveTypeCategory = loveTypeCategory;
        this.avoidanceRate = avoidanceRate;
        this.anxietyRate = anxietyRate;
    }

    public void refreshMemberToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 개인의 startLoveDate 업데이트
     * @deprecated v2에서는 더 이상 개인의 startLoveDate를 업데이트하지 않습니다.
     * 커플의 startLoveDate만 업데이트합니다.
     */
    @Deprecated
    public void updateStartLoveDate(LocalDate startLoveDate) {
        this.startLoveDate = startLoveDate;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    /**
     * Apple 이메일 포워딩 상태를 업데이트합니다.
     */
    public void updateEmailForwardingStatus(EmailForwardingStatus status) {
        this.emailForwardingStatus = status;
    }

    public void delete() {
        this.memberState = MemberState.DELETED;
        this.deletedAt = LocalDateTime.now();
        this.providerId = this.providerId + "_deleted";
    }

    public boolean hasSameRefreshToken(String refreshToken) {
        return this.refreshToken != null && this.refreshToken.equals(refreshToken);
    }

    public void logOut() {
        this.refreshToken = null;
        this.firebaseToken = null;
    }

    public boolean isCoupleLinked() {
        return this.coupleId != null;
    }

    public void linkCouple(CoupleId coupleId) {
        this.coupleId = coupleId;
    }

    public void unlinkCouple() {
        this.coupleId = null;
    }
}
