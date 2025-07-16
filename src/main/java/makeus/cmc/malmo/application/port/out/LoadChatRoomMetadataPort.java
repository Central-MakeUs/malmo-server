package makeus.cmc.malmo.application.port.out;

import makeus.cmc.malmo.domain.value.id.MemberId;

import java.util.Optional;

public interface LoadChatRoomMetadataPort {

    Optional<ChatRoomMetadataDto> loadChatRoomMetadata(MemberId memberId);

    record ChatRoomMetadataDto(String memberLoveTypeTitle, String partnerLoveTypeTitle) {}
}
