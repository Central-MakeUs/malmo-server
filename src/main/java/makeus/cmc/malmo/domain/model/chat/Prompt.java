package makeus.cmc.malmo.domain.model.chat;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.adaptor.out.persistence.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class Prompt extends BaseTimeEntity {

    private Long id;
    private int level;
    private String content;
    private boolean isForMetadata;
    private boolean isLastPromptForMetadata;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public static Prompt from(Long id, int level, String content, boolean isForMetadata, boolean isLastPromptForMetadata,
                                 LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime deletedAt) {
        return Prompt.builder()
                .id(id)
                .level(level)
                .content(content)
                .isForMetadata(isForMetadata)
                .isLastPromptForMetadata(isLastPromptForMetadata)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }
}
