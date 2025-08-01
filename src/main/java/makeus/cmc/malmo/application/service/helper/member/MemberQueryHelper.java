package makeus.cmc.malmo.application.service.helper.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.*;
import makeus.cmc.malmo.domain.exception.*;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberQueryHelper {

    private final LoadMemberPort loadMemberPort;
    private final LoadPartnerPort loadPartnerPort;
    private final LoadInviteCodePort loadInviteCodePort;
    private final ValidateInviteCodePort validateInviteCodePort;
    private final ValidateMemberPort validateMemberPort;

    public Member getMemberByIdOrThrow(MemberId memberId) {
        return loadMemberPort.loadMemberById(MemberId.of(memberId.getValue()))
                .orElseThrow(MemberNotFoundException::new);
    }

    public Member getMemberByInviteCodeOrThrow(InviteCodeValue inviteCode) {
        return loadMemberPort.loadMemberByInviteCode(inviteCode).orElseThrow(InviteCodeNotFoundException::new);
    }

    public InviteCodeValue getInviteCodeByMemberIdOrThrow(MemberId memberId) {
        return loadInviteCodePort.loadInviteCodeByMemberId(memberId)
                .orElseThrow(InviteCodeNotFoundException::new);
    }

    public MemberInfoDto getMemberInfoOrThrow(MemberId memberId) {
        return loadMemberPort.loadMemberDetailsById(memberId).orElseThrow(MemberNotFoundException::new);
    }

    public PartnerMemberDto getPartnerInfoOrThrow(MemberId memberId) {
        return loadPartnerPort.loadPartnerByMemberId(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    public Optional<Member> getMemberByProviderId(Provider provider, String providerId) {
        return loadMemberPort.loadMemberByProviderId(provider, providerId);
    }

    public boolean isInviteCodeValid(InviteCodeValue inviteCode) {
        return !validateInviteCodePort.isInviteCodeDuplicated(inviteCode);
    }

    public boolean isMemberCoupled(MemberId memberId) {
        return validateMemberPort.isCoupleMember(memberId);
    }

    public void validateUsedInviteCode(InviteCodeValue inviteCodeValue) {
        boolean coupleMember = validateInviteCodePort.isAlreadyCoupleMemberByInviteCode(inviteCodeValue);
        if (coupleMember) {
            throw new UsedInviteCodeException("이미 사용된 커플 코드입니다. 다른 코드를 입력해주세요.");
        }
    }

    public void validateMemberNotCoupled(MemberId memberId) {
        boolean coupleMember = validateMemberPort.isCoupleMember(memberId);

        if (coupleMember) {
            throw new AlreadyCoupledMemberException("이미 커플로 등록된 사용자입니다. 커플 등록을 해제 후 이용해주세요.");
        }
    }

    public void validateOwnInviteCode(MemberId memberId, InviteCodeValue inviteCode) {
        Member member = loadMemberPort.loadMemberById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (member.getInviteCode().equals(inviteCode)) {
            throw new NotValidCoupleCodeException("본인의 초대코드를 사용할 수 없습니다.");
        }
    }

    public void isMemberCouple(MemberId memberId) {
        boolean coupleMember = validateMemberPort.isCoupleMember(memberId);

        if (!coupleMember) {
            throw new NotCoupleMemberException("커플 등록 전인 사용자입니다. 커플 등록 후 이용해주세요.");
        }
    }

    public void isMemberValid(MemberId memberId) {
        boolean validMember = validateMemberPort.isValidMember(memberId);

        if (!validMember) {
            throw new MemberNotFoundException("존재하지 않는 사용자입니다. 회원가입 후 이용해주세요.");
        }
    }

    @Data
    @Builder
    public static class MemberInfoDto {
        private String memberState;
        private Provider provider;
        private LocalDate startLoveDate;
        private LoveTypeCategory loveTypeCategory;
        private float avoidanceRate;
        private float anxietyRate;
        private String nickname;
        private String email;

        private int totalChatRoomCount;
        private int totalCoupleQuestionCount;
    }

    @Data
    @Builder
    public static class PartnerMemberDto {
        private String memberState;
        private LoveTypeCategory loveTypeCategory;
        private float avoidanceRate;
        private float anxietyRate;
        private String nickname;
    }

}
