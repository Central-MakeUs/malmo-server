package makeus.cmc.malmo.adaptor.out.persistence.entity.value;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LoveTypePersonalityTypePromptEntityId implements Serializable {
    private String personalityType;
    private LoveTypeCategory loveTypeCategory;
}
