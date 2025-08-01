package makeus.cmc.malmo.application.port.in;

import lombok.Builder;
import lombok.Data;

public interface CoupleUnlinkUseCase {

    void coupleUnlink(CoupleUnlinkCommand command);

    @Data
    @Builder
    class CoupleUnlinkCommand {
        private Long userId;
    }

}
