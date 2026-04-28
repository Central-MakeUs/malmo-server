package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypePersonalityTypeFeatureEntity;
import makeus.cmc.malmo.domain.model.love_type.LoveTypePersonalityTypeFeature;
import org.springframework.stereotype.Component;

@Component
public class LoveTypePersonalityTypeFeatureMapper {

    public LoveTypePersonalityTypeFeature toDomain(LoveTypePersonalityTypeFeatureEntity entity) {
        if (entity == null) {
            return null;
        }

        return LoveTypePersonalityTypeFeature.from(
                entity.getPersonalityType(),
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
                entity.getBestPersonalityType1(),
                entity.getBestDesc1(),
                entity.getBestPersonalityType2(),
                entity.getBestDesc2(),
                entity.getWorstPersonalityType1(),
                entity.getWorstDesc1(),
                entity.getWorstPersonalityType2(),
                entity.getWorstDesc2()
        );
    }
}
