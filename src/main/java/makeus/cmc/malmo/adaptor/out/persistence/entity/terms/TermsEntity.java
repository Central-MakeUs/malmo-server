package makeus.cmc.malmo.adaptor.out.persistence.entity.terms;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import makeus.cmc.malmo.adaptor.out.persistence.entity.BaseTimeEntity;
import makeus.cmc.malmo.domain.value.type.TermsType;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class TermsEntity extends BaseTimeEntity {

    @Column(name = "termsId")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    private float version;

    private boolean isRequired;

    @Enumerated(EnumType.STRING)
    private TermsType termsType;

}
