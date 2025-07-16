package makeus.cmc.malmo.adaptor.out.persistence.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.couple.QCoupleMemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.love_type.QLoveTypeEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.QMemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.application.port.out.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.application.port.out.LoadMemberPort;
import makeus.cmc.malmo.application.port.out.LoadPartnerPort;
import makeus.cmc.malmo.domain.value.state.CoupleMemberState;

import java.util.Optional;

import static makeus.cmc.malmo.adaptor.out.persistence.entity.couple.QCoupleEntity.coupleEntity;
import static makeus.cmc.malmo.adaptor.out.persistence.entity.couple.QCoupleMemberEntity.coupleMemberEntity;
import static makeus.cmc.malmo.adaptor.out.persistence.entity.love_type.QLoveTypeEntity.loveTypeEntity;
import static makeus.cmc.malmo.adaptor.out.persistence.entity.member.QMemberEntity.memberEntity;

@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<LoadMemberPort.MemberResponseRepositoryDto> findMemberDetailsById(Long memberId) {
        LoadMemberPort.MemberResponseRepositoryDto dto = queryFactory
                .select(Projections.constructor(LoadMemberPort.MemberResponseRepositoryDto.class,
                        memberEntity.memberState.stringValue(),
                        coupleEntity.startLoveDate.coalesce(memberEntity.startLoveDate),
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
                        memberEntity.memberState.stringValue(),
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

    @Override
    public boolean isCoupleMember(Long memberId) {
        return queryFactory
                .selectOne()
                .from(coupleMemberEntity)
                .where(coupleMemberEntity.memberEntityId.value.eq(memberId)
                        .and(coupleMemberEntity.coupleMemberState.eq(CoupleMemberState.ALIVE)))
                .fetchFirst() != null;
    }

    @Override
    public boolean existsByInviteCode(String inviteCode) {
        return queryFactory
                .selectFrom(memberEntity)
                .where(memberEntity.inviteCodeEntityValue.value.eq(inviteCode))
                .fetchFirst() != null;
    }

    @Override
    public boolean isAlreadyCoupleMemberByInviteCode(String inviteCode) {
        return queryFactory
                .selectOne()
                .from(coupleMemberEntity)
                .join(memberEntity).on(memberEntity.id.eq(coupleMemberEntity.memberEntityId.value))
                .where(memberEntity.inviteCodeEntityValue.value.eq(inviteCode)
                        .and(coupleMemberEntity.coupleMemberState.eq(CoupleMemberState.ALIVE)))
                .fetchFirst() != null;
    }

    @Override
    public Optional<InviteCodeEntityValue> findInviteCodeByMemberId(Long memberId) {
        InviteCodeEntityValue inviteCodeEntityValue = queryFactory
                .select(memberEntity.inviteCodeEntityValue)
                .from(memberEntity)
                .where(memberEntity.id.eq(memberId))
                .fetchOne();

        return Optional.ofNullable(inviteCodeEntityValue);
    }

    @Override
    public Optional<LoadChatRoomMetadataPort.ChatRoomMetadataDto> loadChatRoomMetadata(Long memberId) {
        QMemberEntity partnerMemberEntity = new QMemberEntity("partnerMemberEntity");
        QCoupleMemberEntity partnerCoupleMemberEntity = new QCoupleMemberEntity("partnerCoupleMemberEntity");
        QLoveTypeEntity partnerLoveTypeEntity = new QLoveTypeEntity("partnerLoveTypeEntity");

        LoadChatRoomMetadataPort.ChatRoomMetadataDto dto = queryFactory
                .select(Projections.constructor(
                        LoadChatRoomMetadataPort.ChatRoomMetadataDto.class,
                        loveTypeEntity.title,
                        partnerLoveTypeEntity.title
                ))
                .from(memberEntity)
                .leftJoin(loveTypeEntity).on(loveTypeEntity.id.eq(memberEntity.loveTypeEntityId.value))
                .leftJoin(coupleMemberEntity).on(coupleMemberEntity.memberEntityId.value.eq(memberEntity.id))
                .leftJoin(coupleEntity).on(coupleEntity.id.eq(coupleMemberEntity.coupleEntityId.value))
                .leftJoin(partnerCoupleMemberEntity)
                    .on(partnerCoupleMemberEntity.coupleEntityId.value.eq(coupleEntity.id)
                            .and(partnerCoupleMemberEntity.memberEntityId.value.ne(memberId)))
                .leftJoin(partnerMemberEntity).on(partnerMemberEntity.id.eq(partnerCoupleMemberEntity.memberEntityId.value))
                .leftJoin(partnerLoveTypeEntity).on(partnerLoveTypeEntity.id.eq(partnerMemberEntity.loveTypeEntityId.value))
                .where(memberEntity.id.eq(memberId))
                .fetchOne();

        return Optional.ofNullable(dto);
    }
}
