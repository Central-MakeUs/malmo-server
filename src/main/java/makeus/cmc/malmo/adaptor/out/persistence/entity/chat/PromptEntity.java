package makeus.cmc.malmo.adaptor.out.persistence.entity.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import makeus.cmc.malmo.adaptor.out.persistence.entity.BaseTimeEntity;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class PromptEntity extends BaseTimeEntity {

    @Column(name = "promptId")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int level;

    @Column(columnDefinition = "TEXT")
    private String content;

    private boolean isForSystem;

    private boolean isForSummary;

    private boolean isForCompletedResponse;

    private boolean isForTotalSummary;

    private boolean isForGuideline;

    private boolean isForAnswerMetadata;

    private boolean isForTitleGeneration;

    private boolean isForWeeklyReport;

}
