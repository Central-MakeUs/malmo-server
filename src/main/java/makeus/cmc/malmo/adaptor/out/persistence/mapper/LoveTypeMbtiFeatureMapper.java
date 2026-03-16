package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypeMbtiFeatureEntity;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeMbtiFeature;
import org.springframework.stereotype.Component;

@Component
public class LoveTypeMbtiFeatureMapper {

    public LoveTypeMbtiFeature toDomain(LoveTypeMbtiFeatureEntity entity) {
        if (entity == null) {
            return null;
        }

        return LoveTypeMbtiFeature.from(
                entity.getMbti(),
                entity.getLoveTypeCategory(),
                entity.getSummary(),
                entity.getKeyword1(),
                entity.getKeyword2(),
                entity.getKeyword3(),
                entity.getStrength1(),
                entity.getStrength2(),
                entity.getStrength3(),
                entity.getWeakness(),
                entity.getStrengthDesc1(),
                entity.getStrengthDesc2(),
                entity.getStrengthDesc3(),
                entity.getWeaknessDesc(),
                entity.getPatternTitle1(),
                entity.getPatternTitle2(),
                entity.getPatternTitle3(),
                entity.getPatternTitle4(),
                entity.getPattern1(),
                entity.getPattern2(),
                entity.getPattern3(),
                entity.getPattern4(),
                entity.getLoveTypeFeatureTitle1(),
                entity.getLoveTypeFeatureTitle2(),
                entity.getLoveTypeFeatureTitle3(),
                entity.getLoveTypeFeatureTitle4(),
                entity.getLoveTypeFeature1(),
                entity.getLoveTypeFeature2(),
                entity.getLoveTypeFeature3(),
                entity.getLoveTypeFeature4(),
                entity.getDatingGuide1(),
                entity.getDatingGuide2(),
                entity.getDatingGuide3(),
                entity.getBestMbti1(),
                entity.getBestDesc1(),
                entity.getBestMbti2(),
                entity.getBestDesc2(),
                entity.getWorstMbti1(),
                entity.getWorstDesc1(),
                entity.getWorstMbti2(),
                entity.getWorstDesc2()
        );
    }
}
