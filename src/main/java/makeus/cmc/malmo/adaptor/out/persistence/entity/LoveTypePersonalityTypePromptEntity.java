package makeus.cmc.malmo.adaptor.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.LoveTypePersonalityTypePromptEntityId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@IdClass(LoveTypePersonalityTypePromptEntityId.class)
@Table(name = "love_type_personality_type_prompt")
public class LoveTypePersonalityTypePromptEntity {

    @Id
    @Column(name = "personality_type")
    private String personalityType;

    @Id
    @Column(name = "lovetype")
    @Enumerated(EnumType.STRING)
    private LoveTypeCategory loveTypeCategory;

    @Column(name = "prompts", columnDefinition = "TEXT")
    private String prompts;
}
