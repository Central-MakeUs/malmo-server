package makeus.cmc.malmo.application.port.out;

import makeus.cmc.malmo.domain.model.love_type.LoveTypeMbtiFeature;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.util.Optional;

public interface LoadLoveTypeMbtiFeaturePort {
    Optional<LoveTypeMbtiFeature> loadByMbtiAndLoveTypeCategory(String mbti, LoveTypeCategory loveTypeCategory);
}
