package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypePersonalityTypePromptEntity;
import makeus.cmc.malmo.domain.model.love_type.LoveTypePersonalityTypePrompt;
import org.springframework.stereotype.Component;

@Component
public class LoveTypePersonalityTypePromptMapper {

    public LoveTypePersonalityTypePrompt toDomain(LoveTypePersonalityTypePromptEntity entity) {
        if (entity == null) {
            return null;
        }

        return LoveTypePersonalityTypePrompt.from(
                entity.getPersonalityType(),
                entity.getLoveTypeCategory(),
                entity.getPrompts()
        );
    }
}
