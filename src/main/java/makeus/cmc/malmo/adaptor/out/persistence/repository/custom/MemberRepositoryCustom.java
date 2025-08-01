package makeus.cmc.malmo.adaptor.out.persistence.repository.custom;

import makeus.cmc.malmo.adaptor.out.persistence.MemberPersistenceAdapter;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.application.port.out.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.application.port.out.LoadPartnerPort;

import java.util.Optional;

public interface MemberRepositoryCustom {
    Optional<MemberPersistenceAdapter.MemberResponseRepositoryDto> findMemberDetailsById(Long memberId);
    Optional<MemberPersistenceAdapter.PartnerMemberRepositoryDto> findPartnerMember(Long memberId);

    boolean isCoupleMember(Long memberId);

    boolean existsByInviteCode(String inviteCode);

    boolean isAlreadyCoupleMemberByInviteCode(String inviteCode);

    Optional<InviteCodeEntityValue> findInviteCodeByMemberId(Long memberId);

    Optional<LoadChatRoomMetadataPort.ChatRoomMetadataDto> loadChatRoomMetadata(Long memberId);

    boolean isMemberStateAlive(Long memberId);

}
