package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.LoveTypeMbtiFeatureMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.LoveTypeMbtiFeatureRepository;
import makeus.cmc.malmo.application.port.out.LoadLoveTypeMbtiFeaturePort;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeMbtiFeature;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoveTypeMbtiFeaturePersistenceAdapter implements LoadLoveTypeMbtiFeaturePort {

    private final LoveTypeMbtiFeatureRepository loveTypeMbtiFeatureRepository;
    private final LoveTypeMbtiFeatureMapper loveTypeMbtiFeatureMapper;

    @Override
    public Optional<LoveTypeMbtiFeature> loadByMbtiAndLoveTypeCategory(String mbti, LoveTypeCategory loveTypeCategory) {
        return loveTypeMbtiFeatureRepository.findByMbtiIgnoreCaseAndLoveTypeCategory(mbti, loveTypeCategory)
                .map(loveTypeMbtiFeatureMapper::toDomain);
    }
}
