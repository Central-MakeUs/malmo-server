package makeus.cmc.malmo.domain.model.question;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class Question {
    private Long id;
    private String title;
    private String content;
    private int level;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public static Question from(Long id, String title, String content, int level, LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime deletedAt) {
        return Question.builder()
                .id(id)
                .title(title)
                .content(content)
                .level(level)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }
}