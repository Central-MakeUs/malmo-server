package makeus.cmc.malmo.adaptor.out.persistence.repository.member;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.out.persistence.adapter.MemberPersistenceAdapter;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.InviteCodeEntityValue;
import makeus.cmc.malmo.application.port.out.chat.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.domain.value.state.CoupleState;
import makeus.cmc.malmo.domain.value.state.MemberState;

import java.util.Optional;

import static makeus.cmc.malmo.adaptor.out.persistence.entity.couple.QCoupleEntity.coupleEntity;
import static makeus.cmc.malmo.adaptor.out.persistence.entity.member.QMemberEntity.memberEntity;

@Slf4j
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<MemberPersistenceAdapter.MemberResponseRepositoryDto> findMemberDetailsById(Long memberId) {
        MemberPersistenceAdapter.MemberResponseRepositoryDto dto = queryFactory
                .select(Projections.constructor(MemberPersistenceAdapter.MemberResponseRepositoryDto.class,
                        memberEntity.memberState.stringValue(),
                        memberEntity.provider,
                        coupleEntity.startLoveDate.coalesce(memberEntity.startLoveDate),
                        memberEntity.loveTypeCategory,
                        memberEntity.avoidanceRate,
                        memberEntity.anxietyRate,
                        memberEntity.nickname,
                        memberEntity.email,
                        memberEntity.relationshipStatus,
                        memberEntity.personalityType,
                        memberEntity.otherPersonalityType,
                        memberEntity.partnerLoveTypeCategory
                ))
                .from(memberEntity)
                .leftJoin(coupleEntity)
                .on(memberEntity.coupleEntityId.value.eq(coupleEntity.id)
                        .and(coupleEntity.coupleState.ne(CoupleState.DELETED)))
                .where(memberEntity.id.eq(memberId))
                .fetchOne();

        return Optional.ofNullable(dto);
    }

    @Override
    public Optional<MemberPersistenceAdapter.PartnerMemberRepositoryDto> findPartnerMember(Long memberId) {
        MemberPersistenceAdapter.PartnerMemberRepositoryDto dto = queryFactory
                .select(Projections.constructor(MemberPersistenceAdapter.PartnerMemberRepositoryDto.class,
                        memberEntity.otherPersonalityType,
                        memberEntity.partnerLoveTypeCategory
                ))
                .from(memberEntity)
                .where(memberEntity.id.eq(memberId)
                        .and(memberEntity.otherPersonalityType.isNotNull()))
                .fetchOne();

        return Optional.ofNullable(dto);
    }

    @Override
    public boolean isCoupleMember(Long memberId) {
        Boolean isCouple = queryFactory.select(memberEntity.coupleEntityId.value.isNotNull())
                .from(memberEntity)
                .where(memberEntity.id.eq(memberId)
                        .and(memberEntity.memberState.ne(MemberState.DELETED)))
                .fetchFirst();

        return isCouple != null && isCouple;
    }

    @Override
    public boolean existsByInviteCode(String inviteCode) {
        return queryFactory
                .selectFrom(memberEntity)
                .where(memberEntity.inviteCodeEntityValue.value.eq(inviteCode))
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
        LoadChatRoomMetadataPort.ChatRoomMetadataDto dto = queryFactory
                .select(Projections.constructor(
                        LoadChatRoomMetadataPort.ChatRoomMetadataDto.class,
                        memberEntity.loveTypeCategory,
                        memberEntity.partnerLoveTypeCategory
                ))
                .from(memberEntity)
                .where(memberEntity.id.eq(memberId))
                .fetchOne();

        return Optional.ofNullable(dto);
    }

    @Override
    public boolean isMemberStateAlive(Long memberId) {
        Long count = queryFactory.select(memberEntity.count())
            .from(memberEntity)
            .where(memberEntity.id.eq(memberId)
                    .and(memberEntity.memberState.ne(MemberState.DELETED)))
            .fetchOne();

        return count != null && count > 0;
    }
}
