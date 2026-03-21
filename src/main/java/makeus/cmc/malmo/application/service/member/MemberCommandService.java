package makeus.cmc.malmo.application.service.member;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.aop.CheckCoupleMember;
import makeus.cmc.malmo.adaptor.in.aop.CheckValidMember;
import makeus.cmc.malmo.application.helper.couple.CoupleCommandHelper;
import makeus.cmc.malmo.application.helper.couple.CoupleQueryHelper;
import makeus.cmc.malmo.application.helper.member.MemberCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberMemoryCommandHelper;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.helper.member.OauthTokenHelper;
import makeus.cmc.malmo.application.helper.notification.MemberNotificationCommandHelper;
import makeus.cmc.malmo.application.port.in.member.CreatePartnerProfileUseCase;
import makeus.cmc.malmo.application.port.in.member.DeleteMemberUseCase;
import makeus.cmc.malmo.application.port.in.member.UpdatePartnerProfileUseCase;
import makeus.cmc.malmo.application.port.in.member.UpdateMemberUseCase;
import makeus.cmc.malmo.application.port.in.member.UpdateStartLoveDateUseCase;
import makeus.cmc.malmo.application.exception.PartnerProfileAlreadyExistsException;
import makeus.cmc.malmo.application.exception.PartnerProfileNotFoundException;
import makeus.cmc.malmo.application.port.out.sse.SendSseEventPort;
import makeus.cmc.malmo.application.port.out.sse.ValidateSsePort;
import makeus.cmc.malmo.domain.model.couple.Couple;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.Provider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static makeus.cmc.malmo.application.port.out.sse.SendSseEventPort.SseEventType.COUPLE_DISCONNECTED;

@Service
@RequiredArgsConstructor
public class MemberCommandService implements
        UpdateMemberUseCase,
        CreatePartnerProfileUseCase,
        UpdatePartnerProfileUseCase,
        UpdateStartLoveDateUseCase,
        DeleteMemberUseCase {

    private final CoupleQueryHelper coupleQueryHelper;
    private final CoupleCommandHelper coupleCommandHelper;

    private final MemberQueryHelper memberQueryHelper;
    private final MemberCommandHelper memberCommandHelper;
    private final OauthTokenHelper oauthTokenHelper;
    private final MemberMemoryCommandHelper memberMemoryCommandHelper;

    private final SendSseEventPort sendSseEventPort;
    private final ValidateSsePort validateSsePort;
    private final MemberNotificationCommandHelper memberNotificationCommandHelper;

    @Override
    @CheckValidMember
    @Transactional
    public UpdateMemberResponseDto updateMember(UpdateMemberCommand command) {
        Member member = memberQueryHelper.getMemberByIdOrThrow(MemberId.of(command.getMemberId()));

        member.updateMemberProfile(
                command.getNickname(),
                command.getRelationshipStatus(),
                command.getPersonalityType(),
                command.getLoveTypeCategory()
        );

        Member savedMember = memberCommandHelper.saveMember(member);

        return UpdateMemberResponseDto.builder()
                .nickname(savedMember.getNickname())
                .relationshipStatus(savedMember.getRelationshipStatus())
                .personalityType(savedMember.getPersonalityType())
                .loveTypeCategory(savedMember.getLoveTypeCategory())
                .build();
    }

    @Override
    @CheckValidMember
    @Transactional
    public PartnerProfileResponseDto createPartnerProfile(CreatePartnerProfileCommand command) {
        Member member = memberQueryHelper.getMemberByIdOrThrow(MemberId.of(command.getMemberId()));
        if (member.hasPartnerProfile()) {
            throw new PartnerProfileAlreadyExistsException("이미 상대 프로필이 등록되어 있습니다.");
        }

        PartnerLoveTypeCategory partnerLoveTypeCategory = resolvePartnerLoveTypeCategory(command.getLoveTypeCategory());
        member.createPartnerProfile(command.getPersonalityType(), partnerLoveTypeCategory);
        Member savedMember = memberCommandHelper.saveMember(member);

        return toPartnerProfileResponse(savedMember);
    }

    @Override
    @CheckValidMember
    @Transactional
    public PartnerProfileResponseDto updatePartnerProfile(UpdatePartnerProfileCommand command) {
        Member member = memberQueryHelper.getMemberByIdOrThrow(MemberId.of(command.getMemberId()));
        if (!member.hasPartnerProfile()) {
            throw new PartnerProfileNotFoundException("등록된 상대 프로필이 없습니다.");
        }

        PartnerLoveTypeCategory partnerLoveTypeCategory = resolvePartnerLoveTypeCategory(command.getLoveTypeCategory());

        member.updatePartnerProfile(command.getPersonalityType(), partnerLoveTypeCategory);
        Member savedMember = memberCommandHelper.saveMember(member);

        return toPartnerProfileResponse(savedMember);
    }

    @Override
    @CheckCoupleMember
    @Transactional
    public UpdateStartLoveDateUseCase.UpdateStartLoveDateResponse updateStartLoveDate(UpdateStartLoveDateUseCase.UpdateStartLoveDateCommand command) {
        Member member = memberQueryHelper.getMemberByIdOrThrow(MemberId.of(command.getMemberId()));

        LocalDate startLoveDate = command.getStartLoveDate();

        // 커플의 startLoveDate만 업데이트 (개인의 startLoveDate는 업데이트하지 않음)
        Couple couple = coupleQueryHelper.getCoupleByIdOrThrow(member.getCoupleId());
        couple.updateStartLoveDate(startLoveDate);
        coupleCommandHelper.saveCouple(couple);

        return UpdateStartLoveDateUseCase.UpdateStartLoveDateResponse.builder()
                .startLoveDate(couple.getStartLoveDate())
                .build();
    }

    @Override
    @CheckValidMember
    @Transactional
    public void deleteMember(DeleteMemberCommand command) {
        Member member = memberQueryHelper.getMemberByIdOrThrow(MemberId.of(command.getMemberId()));
        // OAuth를 통한 계정 연결 해제
        if (member.getProvider() == Provider.KAKAO) {
            oauthTokenHelper.unlinkKakao(member.getProviderId());
        } else if (member.getProvider() == Provider.APPLE) {
            oauthTokenHelper.unlinkApple(member.getOauthToken());
        }

        // 멤버 soft delete
        member.delete();
        memberCommandHelper.saveMember(member);

        // 멤버 채팅방, 커플 soft delete
        coupleQueryHelper.getCoupleByMemberId(MemberId.of(command.getMemberId()))
                        .ifPresent(couple -> coupleUnlink(member, couple));
    }

    public void coupleUnlink(Member member, Couple couple) {
        MemberId partnerId = couple.getOtherMemberId(MemberId.of(member.getId()));

        // 사용자의 커플 메모리 제거
        memberMemoryCommandHelper.deleteCoupleMemberMemory(member.getCoupleId(), MemberId.of(member.getId()));

        // 커플 해제 처리
        couple.unlink(MemberId.of(member.getId()),
                member.getNickname(),
                member.getLoveTypeCategory(),
                member.getAnxietyRate(),
                member.getAvoidanceRate());
        coupleCommandHelper.saveCouple(couple);

        member.unlinkCouple();
        memberCommandHelper.saveMember(member);

        // 상대방에게 커플 해지됨 알림
        if (validateSsePort.isMemberOnline(partnerId)) {
            sendSseEventPort.sendToMember(partnerId,
                    new SendSseEventPort.NotificationEvent(COUPLE_DISCONNECTED, couple.getId())
            );
        } else {
            // 상대방이 온라인이 아닐 경우 알림 생성
            memberNotificationCommandHelper.createAndSaveCoupleDisconnectedNotification(partnerId);
        }
    }

    private PartnerProfileResponseDto toPartnerProfileResponse(Member savedMember) {
        return PartnerProfileResponseDto.builder()
                .personalityType(savedMember.getOtherPersonalityType())
                .loveTypeCategory(savedMember.getPartnerLoveTypeCategory())
                .description(savedMember.getPartnerLoveTypeCategory() == null
                        ? null
                        : savedMember.getPartnerLoveTypeCategory().getDescription())
                .build();
    }

    private PartnerLoveTypeCategory resolvePartnerLoveTypeCategory(PartnerLoveTypeCategory loveTypeCategory) {
        return loveTypeCategory == null ? PartnerLoveTypeCategory.UNKNOWN : loveTypeCategory;
    }
}
