package makeus.cmc.malmo.application.service;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.exception.LoveTypePersonalityTypeFeatureNotFoundException;
import makeus.cmc.malmo.application.port.in.GetLoveTypePersonalityTypeResultUseCase;
import makeus.cmc.malmo.application.port.out.LoadLoveTypePersonalityTypeFeaturePort;
import makeus.cmc.malmo.domain.model.love_type.LoveTypePersonalityTypeFeature;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LoveTypePersonalityTypeFeatureService implements GetLoveTypePersonalityTypeResultUseCase {

    private final LoadLoveTypePersonalityTypeFeaturePort loadLoveTypePersonalityTypeFeaturePort;

    @Override
    public LoveTypePersonalityTypeResultResponse getResult(GetLoveTypePersonalityTypeResultCommand command) {
        LoveTypePersonalityTypeFeature feature = loadLoveTypePersonalityTypeFeaturePort
                .loadByPersonalityTypeAndLoveTypeCategory(command.getPersonalityType(), command.getLoveTypeCategory())
                .orElseThrow(LoveTypePersonalityTypeFeatureNotFoundException::new);

        return LoveTypePersonalityTypeResultResponse.builder()
                .personalityType(feature.getPersonalityType())
                .loveTypeCategory(feature.getLoveTypeCategory())
                .summary(feature.getSummary())
                .keywords(buildStringList(feature.getKeyword1(), feature.getKeyword2(), feature.getKeyword3()))
                .strengths(buildTitleDescriptionItems(
                        feature.getStrength1(), feature.getStrengthDesc1(),
                        feature.getStrength2(), feature.getStrengthDesc2(),
                        feature.getStrength3(), feature.getStrengthDesc3()
                ))
                .weaknesses(buildTitleDescriptionItems(feature.getWeakness(), feature.getWeaknessDesc()))
                .patterns(buildTitleDescriptionItems(
                        feature.getPatternTitle1(), feature.getPattern1(),
                        feature.getPatternTitle2(), feature.getPattern2(),
                        feature.getPatternTitle3(), feature.getPattern3(),
                        feature.getPatternTitle4(), feature.getPattern4()
                ))
                .loveTypeFeatures(buildTitleDescriptionItems(
                        feature.getLoveTypeFeatureTitle1(), feature.getLoveTypeFeature1(),
                        feature.getLoveTypeFeatureTitle2(), feature.getLoveTypeFeature2(),
                        feature.getLoveTypeFeatureTitle3(), feature.getLoveTypeFeature3(),
                        feature.getLoveTypeFeatureTitle4(), feature.getLoveTypeFeature4()
                ))
                .datingGuides(buildStringList(
                        feature.getDatingGuide1(),
                        feature.getDatingGuide2(),
                        feature.getDatingGuide3()
                ))
                .bestMatches(buildPersonalityTypeDescriptionItems(
                        feature.getBestPersonalityType1(), feature.getBestDesc1(),
                        feature.getBestPersonalityType2(), feature.getBestDesc2()
                ))
                .worstMatches(buildPersonalityTypeDescriptionItems(
                        feature.getWorstPersonalityType1(), feature.getWorstDesc1(),
                        feature.getWorstPersonalityType2(), feature.getWorstDesc2()
                ))
                .build();
    }

    private List<String> buildStringList(String... values) {
        return Stream.of(values)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<TitleDescriptionItem> buildTitleDescriptionItems(String... values) {
        return Stream.iterate(0, index -> index < values.length, index -> index + 2)
                .map(index -> TitleDescriptionItem.builder()
                        .title(normalizeBlank(values[index]))
                        .description(normalizeBlank(values[index + 1]))
                        .build())
                .filter(item -> StringUtils.hasText(item.getTitle()) || StringUtils.hasText(item.getDescription()))
                .toList();
    }

    private List<PersonalityTypeDescriptionItem> buildPersonalityTypeDescriptionItems(String... values) {
        return Stream.iterate(0, index -> index < values.length, index -> index + 2)
                .map(index -> PersonalityTypeDescriptionItem.builder()
                        .personalityType(normalizeBlank(values[index]))
                        .description(normalizeBlank(values[index + 1]))
                        .build())
                .filter(item -> StringUtils.hasText(item.getPersonalityType()) || StringUtils.hasText(item.getDescription()))
                .toList();
    }

    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
