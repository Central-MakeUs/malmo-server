package makeus.cmc.malmo.adaptor.out.persistence.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.port.out.LoadMemberPort;
import makeus.cmc.malmo.application.port.out.LoadPartnerPort;

import java.util.Optional;

import static makeus.cmc.malmo.adaptor.out.persistence.entity.couple.QCoupleEntity.coupleEntity;
import static makeus.cmc.malmo.adaptor.out.persistence.entity.couple.QCoupleMemberEntity.coupleMemberEntity;
import static makeus.cmc.malmo.adaptor.out.persistence.entity.love_type.QLoveTypeEntity.loveTypeEntity;
import static makeus.cmc.malmo.adaptor.out.persistence.entity.member.QMemberEntity.memberEntity;

@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<LoadMemberPort.MemberResponseRepositoryDto> findMemberDetailsById(Long memberId) {
        LoadMemberPort.MemberResponseRepositoryDto dto = queryFactory
                .select(Projections.constructor(LoadMemberPort.MemberResponseRepositoryDto.class,
                        memberEntity.memberStateJpa.stringValue(),
                        coupleEntity.startLoveDate,
                        loveTypeEntity.id,
                        loveTypeEntity.title,
                        memberEntity.avoidanceRate,
                        memberEntity.anxietyRate,
                        memberEntity.nickname,
                        memberEntity.email
                ))
                .from(memberEntity)
                .leftJoin(loveTypeEntity).on(loveTypeEntity.id.eq(memberEntity.loveTypeEntityId.value))
                .leftJoin(coupleMemberEntity).on(coupleMemberEntity.memberEntityId.value.eq(memberEntity.id))
                .leftJoin(coupleEntity).on(coupleEntity.id.eq(coupleMemberEntity.coupleEntityId.value))
                .where(memberEntity.id.eq(memberId))
                .fetchOne();

        return Optional.ofNullable(dto);
    }

    @Override
    public Optional<LoadPartnerPort.PartnerMemberRepositoryDto> findPartnerMember(Long memberId) {
        LoadPartnerPort.PartnerMemberRepositoryDto dto = queryFactory
                .select(Projections.constructor(LoadPartnerPort.PartnerMemberRepositoryDto.class,
                        memberEntity.memberStateJpa.stringValue(),
                        loveTypeEntity.id,
                        loveTypeEntity.title,
                        memberEntity.avoidanceRate,
                        memberEntity.anxietyRate,
                        memberEntity.nickname
                ))
                .from(coupleEntity)
                .join(coupleEntity.coupleMembers, coupleMemberEntity)
                .join(memberEntity).on(memberEntity.id.eq(coupleMemberEntity.memberEntityId.value))
                .leftJoin(loveTypeEntity).on(loveTypeEntity.id.eq(memberEntity.loveTypeEntityId.value))
                .where(
                        coupleEntity.coupleMembers.any().memberEntityId.value.eq(memberId)
                                .and(coupleMemberEntity.memberEntityId.value.ne(memberId))
                )
                .fetchOne();
        return Optional.ofNullable(dto);
    }
}
