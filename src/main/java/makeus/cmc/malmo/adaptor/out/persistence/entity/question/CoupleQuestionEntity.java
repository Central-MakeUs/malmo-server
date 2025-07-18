package makeus.cmc.malmo.adaptor.out.persistence.entity.question;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import makeus.cmc.malmo.adaptor.out.persistence.entity.BaseTimeEntityJpa;
import makeus.cmc.malmo.adaptor.out.persistence.entity.couple.CoupleEntity;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class CoupleQuestionEntity extends BaseTimeEntityJpa {

    @Column(name = "coupleQuestionId")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    @ManyToOne
    @JoinColumn(name = "couple_id")
    private CoupleEntity couple;

    @Enumerated(EnumType.STRING)
    private CoupleQuestionStateJpa coupleQuestionStateJpa;
}
