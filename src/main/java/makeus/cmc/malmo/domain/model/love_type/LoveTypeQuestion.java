package makeus.cmc.malmo.domain.model.love_type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.type.LoveTypeQuestionType;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class LoveTypeQuestion {

    private Long id;
    private int questionNumber;
    private boolean isReversed;
    private String content;
    private LoveTypeQuestionType loveTypeQuestionType;
    private int weight;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public boolean isAnxietyType() {
        return loveTypeQuestionType == LoveTypeQuestionType.ANXIETY;
    }

    public int getScore(int score) {
        if (isReversed) {
            return 6 - score;
        }
        return score;
    }

    public static LoveTypeQuestion from(Long id, int questionNumber, boolean isReversed, String content,
                                         LoveTypeQuestionType loveTypeQuestionType, int weight,
                                         LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime deletedAt) {
        return LoveTypeQuestion.builder()
                .id(id)
                .questionNumber(questionNumber)
                .isReversed(isReversed)
                .content(content)
                .loveTypeQuestionType(loveTypeQuestionType)
                .weight(weight)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public void updateByAdmin(String content, int questionNumber, boolean isReversed, LoveTypeQuestionType loveTypeQuestionType) {
        this.content = content;
        this.questionNumber = questionNumber;
        this.isReversed = isReversed;
        this.loveTypeQuestionType = loveTypeQuestionType;
    }
}
