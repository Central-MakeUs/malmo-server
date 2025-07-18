package makeus.cmc.malmo.domain.model.question;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import makeus.cmc.malmo.domain.model.BaseTimeEntity;

@Getter
@SuperBuilder
@AllArgsConstructor
public class Question extends BaseTimeEntity {
    private Long id;
    private String title;
    private String content;
}