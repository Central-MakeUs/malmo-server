package makeus.cmc.malmo.adaptor.out.persistence.entity.value;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LoveTypeMbtiPromptEntityId implements Serializable {
    private String mbti;
    private LoveTypeCategory loveTypeCategory;
}
