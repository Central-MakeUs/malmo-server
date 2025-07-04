package makeus.cmc.malmo.adaptor.out.persistence.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.couple.CoupleCodeEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.MemberEntityId;
import makeus.cmc.malmo.domain.model.member.CoupleCode;
import makeus.cmc.malmo.domain.model.value.MemberId;
import org.springframework.stereotype.Component;

@Component
public class CoupleCodeMapper {
    public CoupleCode toDomain(CoupleCodeEntity entity) {
        return CoupleCode.builder()
                .id(entity.getId())
                .inviteCode(entity.getInviteCode())
                .startLoveDate(entity.getStartLoveDate())
                .memberId(MemberId.of(entity.getMemberEntityId().getValue()))
                .createdAt(entity.getCreatedAt())
                .modifiedAt(entity.getModifiedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public CoupleCodeEntity toEntity(CoupleCode domain) {
        return CoupleCodeEntity.builder()
                .id(domain.getId())
                .inviteCode(domain.getInviteCode())
                .startLoveDate(domain.getStartLoveDate())
                .memberEntityId(MemberEntityId.of(domain.getMemberId().getValue()))
                .createdAt(domain.getCreatedAt())
                .modifiedAt(domain.getModifiedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
