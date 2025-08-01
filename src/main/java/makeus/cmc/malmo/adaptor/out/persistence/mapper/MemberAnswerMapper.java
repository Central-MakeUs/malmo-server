package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.question.MemberAnswerEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.CoupleMemberEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.CoupleQuestionEntityId;
import makeus.cmc.malmo.domain.model.question.MemberAnswer;
import makeus.cmc.malmo.domain.value.id.CoupleMemberId;
import makeus.cmc.malmo.domain.value.id.CoupleQuestionId;
import org.springframework.stereotype.Component;

@Component
public class MemberAnswerMapper {

    public MemberAnswer toDomain(MemberAnswerEntity memberAnswerEntity) {
        return MemberAnswer.from(
                memberAnswerEntity.getId(),
                memberAnswerEntity.getCoupleQuestionEntityId() != null ?
                        CoupleQuestionId.of(memberAnswerEntity.getCoupleQuestionEntityId().getValue()) : null,
                memberAnswerEntity.getCoupleMemberEntityId() != null ?
                        CoupleMemberId.of(memberAnswerEntity.getCoupleMemberEntityId().getValue()) : null,
                memberAnswerEntity.getAnswer(),
                memberAnswerEntity.getMemberAnswerState(),
                memberAnswerEntity.getCreatedAt(),
                memberAnswerEntity.getModifiedAt(),
                memberAnswerEntity.getDeletedAt()
        );
    }

    public MemberAnswerEntity toEntity(MemberAnswer memberAnswer) {
        return MemberAnswerEntity.builder()
                .id(memberAnswer.getId())
                .coupleQuestionEntityId(memberAnswer.getCoupleQuestionId() != null ?
                        CoupleQuestionEntityId.of(memberAnswer.getCoupleQuestionId().getValue()) : null)
                .coupleMemberEntityId(memberAnswer.getCoupleMemberId() != null ?
                        CoupleMemberEntityId.of(memberAnswer.getCoupleMemberId().getValue()) : null)
                .answer(memberAnswer.getAnswer())
                .memberAnswerState(memberAnswer.getMemberAnswerState())
                .createdAt(memberAnswer.getCreatedAt())
                .modifiedAt(memberAnswer.getModifiedAt())
                .deletedAt(memberAnswer.getDeletedAt())
                .build();
    }
}
