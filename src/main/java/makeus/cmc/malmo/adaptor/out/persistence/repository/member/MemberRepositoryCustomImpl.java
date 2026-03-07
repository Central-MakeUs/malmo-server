package makeus.cmc.malmo.adaptor.out.persistence.repository.member;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.out.persistence.adapter.MemberPersistenceAdapter;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.QMemberEntity;
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
                        memberEntity.otherPersonalityType
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
        QMemberEntity partnerMemberEntity = new QMemberEntity("partnerMemberEntity");

        MemberPersistenceAdapter.PartnerMemberRepositoryDto dto = queryFactory
                .select(Projections.constructor(MemberPersistenceAdapter.PartnerMemberRepositoryDto.class,
                        coupleEntity.coupleState.stringValue(),
                        // 파트너가 실제 멤버면 memberEntity 정보, 아니면 스냅샷 정보 사용
                        new CaseBuilder()
                                .when(partnerMemberEntity.coupleEntityId.value.eq(coupleEntity.id))
                                .then(partnerMemberEntity.loveTypeCategory)
                                .otherwise(
                                        new CaseBuilder()
                                                .when(coupleEntity.firstMemberId.value.eq(memberId))
                                                .then(coupleEntity.secondMemberSnapshot.loveTypeCategory)
                                                .otherwise(coupleEntity.firstMemberSnapshot.loveTypeCategory)
                                ),
                        new CaseBuilder()
                                .when(partnerMemberEntity.coupleEntityId.value.eq(coupleEntity.id))
                                .then(partnerMemberEntity.avoidanceRate)
                                .otherwise(
                                        new CaseBuilder()
                                                .when(coupleEntity.firstMemberId.value.eq(memberId))
                                                .then(coupleEntity.secondMemberSnapshot.avoidanceRate)
                                                .otherwise(coupleEntity.firstMemberSnapshot.avoidanceRate)
                                ),
                        new CaseBuilder()
                                .when(partnerMemberEntity.coupleEntityId.value.eq(coupleEntity.id))
                                .then(partnerMemberEntity.anxietyRate)
                                .otherwise(
                                        new CaseBuilder()
                                                .when(coupleEntity.firstMemberId.value.eq(memberId))
                                                .then(coupleEntity.secondMemberSnapshot.anxietyRate)
                                                .otherwise(coupleEntity.firstMemberSnapshot.anxietyRate)
                                ),
                        new CaseBuilder()
                                .when(partnerMemberEntity.coupleEntityId.value.eq(coupleEntity.id))
                                .then(partnerMemberEntity.nickname)
                                .otherwise(
                                        new CaseBuilder()
                                                .when(coupleEntity.firstMemberId.value.eq(memberId))
                                                .then(coupleEntity.secondMemberSnapshot.nickname)
                                                .otherwise(coupleEntity.firstMemberSnapshot.nickname)
                                ),
                        coupleEntity.isStartLoveDateUpdated
                ))
                .from(memberEntity)
                .join(coupleEntity).on(memberEntity.coupleEntityId.value.eq(coupleEntity.id))
                .leftJoin(partnerMemberEntity).on(
                        partnerMemberEntity.id.eq(
                                new CaseBuilder()
                                        .when(coupleEntity.firstMemberId.value.eq(memberId))
                                        .then(coupleEntity.secondMemberId.value)
                                        .otherwise(coupleEntity.firstMemberId.value)
                        )
                )
                .where(memberEntity.id.eq(memberId))
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
        QMemberEntity partnerMemberEntity = new QMemberEntity("partnerMemberEntity");

        LoadChatRoomMetadataPort.ChatRoomMetadataDto dto = queryFactory
                .select(Projections.constructor(
                        LoadChatRoomMetadataPort.ChatRoomMetadataDto.class,
                        memberEntity.loveTypeCategory,
                        new CaseBuilder()
                                .when(partnerMemberEntity.coupleEntityId.value.eq(coupleEntity.id))
                                .then(partnerMemberEntity.loveTypeCategory)
                                .otherwise(
                                        new CaseBuilder()
                                                .when(coupleEntity.firstMemberId.value.eq(memberId))
                                                .then(coupleEntity.secondMemberSnapshot.loveTypeCategory)
                                                .otherwise(coupleEntity.firstMemberSnapshot.loveTypeCategory)
                                )
                ))
                .from(memberEntity)
                .leftJoin(coupleEntity).on(memberEntity.coupleEntityId.value.eq(coupleEntity.id))
                .leftJoin(partnerMemberEntity).on(
                        partnerMemberEntity.id.eq(
                                new CaseBuilder()
                                        .when(coupleEntity.firstMemberId.value.eq(memberId))
                                        .then(coupleEntity.secondMemberId.value)
                                        .otherwise(coupleEntity.firstMemberId.value)
                        )
                )
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
