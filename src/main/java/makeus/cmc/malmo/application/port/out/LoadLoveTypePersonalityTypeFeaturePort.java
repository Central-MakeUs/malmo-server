package makeus.cmc.malmo.application.port.out;

import makeus.cmc.malmo.domain.model.love_type.LoveTypePersonalityTypeFeature;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.util.Optional;

public interface LoadLoveTypePersonalityTypeFeaturePort {
    Optional<LoveTypePersonalityTypeFeature> loadByPersonalityTypeAndLoveTypeCategory(
            String personalityType,
            LoveTypeCategory loveTypeCategory
    );
}
