package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypeMbtiPromptEntity;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeMbtiPrompt;
import org.springframework.stereotype.Component;

@Component
public class LoveTypeMbtiPromptMapper {

    public LoveTypeMbtiPrompt toDomain(LoveTypeMbtiPromptEntity entity) {
        if (entity == null) {
            return null;
        }

        return LoveTypeMbtiPrompt.from(
                entity.getMbti(),
                entity.getLoveTypeCategory(),
                entity.getPrompts()
        );
    }
}
