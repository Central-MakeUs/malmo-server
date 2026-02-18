package makeus.cmc.malmo.application.helper.chat_room;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.repository.chat.MemberChatRoomMetadataRepository;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberChatRoomMetadataMapper;
import makeus.cmc.malmo.domain.model.chat.MemberChatRoomMetadata;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberChatRoomMetadataCommandHelper {

    private final MemberChatRoomMetadataRepository memberChatRoomMetadataRepository;
    private final MemberChatRoomMetadataMapper memberChatRoomMetadataMapper;

    public MemberChatRoomMetadata saveMemberChatRoomMetadata(MemberChatRoomMetadata memberChatRoomMetadata) {
        var entity = memberChatRoomMetadataMapper.toEntity(memberChatRoomMetadata);
        var savedEntity = memberChatRoomMetadataRepository.save(entity);
        return memberChatRoomMetadataMapper.toDomain(savedEntity);
    }

    public MemberChatRoomMetadata saveMemberChatRoomMetadataIfAbsent(MemberChatRoomMetadata memberChatRoomMetadata) {
        boolean exists = memberChatRoomMetadataRepository.existsByChatRoomIdAndMemberIdAndLevelAndDetailedLevel(
                memberChatRoomMetadata.getChatRoomId().getValue(),
                memberChatRoomMetadata.getMemberId().getValue(),
                memberChatRoomMetadata.getLevel(),
                memberChatRoomMetadata.getDetailedLevel());
        if (exists) {
            return memberChatRoomMetadata;
        }
        var entity = memberChatRoomMetadataMapper.toEntity(memberChatRoomMetadata);
        var savedEntity = memberChatRoomMetadataRepository.save(entity);
        return memberChatRoomMetadataMapper.toDomain(savedEntity);
    }
}
