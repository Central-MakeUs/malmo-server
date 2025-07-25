package makeus.cmc.malmo.domain.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.out.LoadLoveTypeQuestionDataPort;
import makeus.cmc.malmo.domain.exception.LoveTypeNotFoundException;
import makeus.cmc.malmo.domain.exception.LoveTypeQuestionNotFoundException;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeQuestionData;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class LoveTypeDataService {

    private final LoadLoveTypeQuestionDataPort loadLoveTypeQuestionDataPort;
    private Map<Long, LoveTypeQuestionData> questionMap;

    @PostConstruct
    public void init() {
        questionMap = loadLoveTypeQuestionDataPort.loadLoveTypeData();
    }

    public LoveTypeCalculationResult findLoveTypeCategoryByTestResult(List<TestResultInput> results) {
        float anxietyScore = 0.0f;
        float avoidanceScore = 0.0f;

        for (TestResultInput result : results) {
            LoveTypeQuestionData question = questionMap.get(result.questionId());
            if (question == null) {
                throw new LoveTypeQuestionNotFoundException();
            }

            if (question.isAnxietyType()) {
                anxietyScore += question.getScore(result.score());
            } else {
                avoidanceScore += question.getScore(result.score());
            }
        }

        anxietyScore = (float) Math.floor((anxietyScore / 18.0f * 100.0f)) / 100.0f; // 소수 셋째 자리에서 내림
        avoidanceScore = (float) Math.floor((avoidanceScore / 18.0f * 100.0f)) / 100.0f;

        LoveTypeCategory category = matchLoveType(anxietyScore, avoidanceScore);

        return new LoveTypeCalculationResult(
                category,
                avoidanceScore,
                anxietyScore
        );

    }

    private LoveTypeCategory matchLoveType(float anxietyScore, float avoidanceScore) {
        LoveTypeCategory[] loveTypeCategories = LoveTypeCategory.values();

        for (LoveTypeCategory loveTypeCategory : loveTypeCategories) {
            if (anxietyScore >= loveTypeCategory.getAnxietyOver() &&
                anxietyScore < loveTypeCategory.getAnxietyUnder() &&
                avoidanceScore >= loveTypeCategory.getAvoidanceOver() &&
                avoidanceScore < loveTypeCategory.getAvoidanceUnder()) {
                return loveTypeCategory;
            }
        }

        throw new LoveTypeNotFoundException();
    }

    public record TestResultInput(Long questionId, int score) {}

    public record LoveTypeCalculationResult(LoveTypeCategory category, float avoidanceScore, float anxietyScore) {}

}
