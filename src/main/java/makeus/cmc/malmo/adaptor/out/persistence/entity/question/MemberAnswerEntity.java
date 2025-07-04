package makeus.cmc.malmo.adaptor.out.persistence.entity.question;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import makeus.cmc.malmo.adaptor.out.persistence.entity.BaseTimeEntityJpa;
import makeus.cmc.malmo.adaptor.out.persistence.entity.couple.CoupleMemberEntity;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class MemberAnswerEntity extends BaseTimeEntityJpa {

    @Column(name = "memberAnswerId")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "couple_question_id")
    private CoupleQuestionEntity coupleQuestion;

    @ManyToOne
    @JoinColumn(name = "couple_member_id")
    private CoupleMemberEntity coupleMember;

    private String answer;

    @Enumerated(EnumType.STRING)
    private MemberAnswerStateJpa memberAnswerStateJpa;

}
