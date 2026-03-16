package makeus.cmc.malmo.application.port.out.chat;

import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;

import java.util.Optional;

public interface LoadChatRoomMetadataPort {

    Optional<ChatRoomMetadataDto> loadChatRoomMetadata(MemberId memberId);

    record ChatRoomMetadataDto(LoveTypeCategory memberLoveType, PartnerLoveTypeCategory partnerLoveType) {}
}
