package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.LoveTypePersonalityTypeFeatureMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.LoveTypePersonalityTypeFeatureRepository;
import makeus.cmc.malmo.application.port.out.LoadLoveTypePersonalityTypeFeaturePort;
import makeus.cmc.malmo.domain.model.love_type.LoveTypePersonalityTypeFeature;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoveTypePersonalityTypeFeaturePersistenceAdapter implements LoadLoveTypePersonalityTypeFeaturePort {

    private final LoveTypePersonalityTypeFeatureRepository loveTypePersonalityTypeFeatureRepository;
    private final LoveTypePersonalityTypeFeatureMapper loveTypePersonalityTypeFeatureMapper;

    @Override
    public Optional<LoveTypePersonalityTypeFeature> loadByPersonalityTypeAndLoveTypeCategory(
            String personalityType,
            LoveTypeCategory loveTypeCategory
    ) {
        return loveTypePersonalityTypeFeatureRepository
                .findByPersonalityTypeIgnoreCaseAndLoveTypeCategory(personalityType, loveTypeCategory)
                .map(loveTypePersonalityTypeFeatureMapper::toDomain);
    }
}
