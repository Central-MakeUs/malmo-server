package makeus.cmc.malmo.domain.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.exception.NotCoupleMemberException;
import makeus.cmc.malmo.application.port.out.*;
import makeus.cmc.malmo.domain.exception.AlreadyCoupledMemberException;
import makeus.cmc.malmo.domain.exception.InviteCodeGenerateFailedException;
import makeus.cmc.malmo.domain.exception.InviteCodeNotFoundException;
import makeus.cmc.malmo.domain.exception.UsedInviteCodeException;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.id.MemberId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InviteCodeDomainService {

    private final LoadMemberPort loadMemberPort;
    private final LoadInviteCodePort loadInviteCodePort;
    private final ValidateInviteCodePort validateInviteCodePort;
    private final ValidateMemberPort validateMemberPort;

    private final GenerateInviteCodePort generateInviteCodePort;

    private static final int MAX_RETRY = 10;

    public Member getMemberByInviteCode(InviteCodeValue inviteCode) {
        return loadMemberPort.loadMemberByInviteCode(inviteCode)
                .orElseThrow(InviteCodeNotFoundException::new);
    }

    public InviteCodeValue getInviteCodeByMemberId(MemberId memberId) {
        return loadInviteCodePort.loadInviteCodeByMemberId(memberId)
                .orElseThrow(InviteCodeNotFoundException::new);
    }

    public InviteCodeValue generateUniqueInviteCode() {
        int retryCount = 0;
        while (retryCount < MAX_RETRY) {
            String inviteCode = generateInviteCodePort.generateInviteCode();
            if (!validateInviteCodePort.validateDuplicateInviteCode(InviteCodeValue.of(inviteCode))) {
                return InviteCodeValue.of(inviteCode);
            }
            retryCount++;
        }

        throw new InviteCodeGenerateFailedException("초대 코드 생성에 실패했습니다. 재시도 횟수를 초과했습니다.");
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

}
