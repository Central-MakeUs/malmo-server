package makeus.cmc.malmo.adaptor.out.persistence.adapter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.persistence.entity.member.MemberEntity;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.chat.ChatRoomRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.member.MemberRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.question.CoupleQuestionRepository;
import makeus.cmc.malmo.application.helper.member.MemberQueryHelper;
import makeus.cmc.malmo.application.port.out.chat.LoadChatRoomMetadataPort;
import makeus.cmc.malmo.application.port.out.member.LoadInviteCodePort;
import makeus.cmc.malmo.application.port.out.member.LoadMemberPort;
import makeus.cmc.malmo.application.port.out.member.LoadPartnerPort;
import makeus.cmc.malmo.application.port.out.member.SaveMemberPort;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.id.CoupleId;
import makeus.cmc.malmo.domain.value.id.InviteCodeValue;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.PartnerLoveTypeCategory;
import makeus.cmc.malmo.domain.value.type.Provider;
import makeus.cmc.malmo.domain.value.type.RelationshipStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements
        LoadMemberPort, SaveMemberPort, LoadPartnerPort, LoadInviteCodePort, LoadChatRoomMetadataPort {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    private final ChatRoomRepository chatRoomRepository;
    private final CoupleQuestionRepository coupleQuestionRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public CoupleId loadCoupleIdByMemberId(MemberId memberId) {
        return CoupleId.of(memberRepository.findCoupleIdByMemberId(memberId.getValue()));
    }

    @Override
    public Optional<Member> loadMemberByProviderId(Provider provider, String providerId) {
        return memberRepository.findByProviderAndProviderId(provider, providerId)
                .map(memberMapper::toDomain);
    }

    @Override
    public Optional<Member> loadMemberById(MemberId memberId) {
        return memberRepository.findById(memberId.getValue())
                .map(memberMapper::toDomain);
    }

    @Override
    public Optional<MemberQueryHelper.MemberInfoDto> loadMemberDetailsById(MemberId memberId) {
        int questionCount = coupleQuestionRepository.countCoupleQuestionsByMemberId(memberId.getValue());
        int chatRoomCount = chatRoomRepository.countChatRoomsByMemberId(memberId.getValue());
        return memberRepository.findMemberDetailsById(memberId.getValue())
                .map(dto -> dto.toDto(chatRoomCount, questionCount));
    }

    @Override
    public Optional<Member> loadMemberByInviteCode(InviteCodeValue inviteCode) {
        return memberRepository.findMemberEntityByInviteCode(inviteCode.getValue())
                .map(memberMapper::toDomain);
    }

    @Override
    public Member saveMember(Member member) {
        MemberEntity memberEntity = memberMapper.toEntity(member);
        MemberEntity savedEntity = memberRepository.save(memberEntity);
        return memberMapper.toDomain(savedEntity);
    }

    @Override
    public Member saveMemberIfAbsent(Member member) {
        try {
            return executeInNewTransaction(() -> {
                MemberEntity memberEntity = memberMapper.toEntity(member);
                MemberEntity savedEntity = memberRepository.saveAndFlush(memberEntity);
                return memberMapper.toDomain(savedEntity);
            });
        } catch (DataIntegrityViolationException exception) {
            return executeInNewTransaction(() -> memberRepository
                    .findByProviderAndProviderId(member.getProvider(), member.getProviderId())
                    .map(memberMapper::toDomain)
                    .orElseThrow(() -> exception));
        }
    }

    private <T> T executeInNewTransaction(Supplier<T> operation) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return Objects.requireNonNull(transactionTemplate.execute(status -> operation.get()));
    }

    @Override
    public Optional<MemberQueryHelper.PartnerMemberDto> loadPartnerByMemberId(MemberId memberId) {
        return memberRepository.findPartnerMember(memberId.getValue())
                .map(PartnerMemberRepositoryDto::toDto);
    }

    @Override
    public Optional<InviteCodeValue> loadInviteCodeByMemberId(MemberId memberId) {
        return memberRepository.findInviteCodeByMemberId(memberId.getValue())
                .map(code -> InviteCodeValue.of(code.getValue()));
    }

    @Override
    public Optional<ChatRoomMetadataDto> loadChatRoomMetadata(MemberId memberId) {
        return memberRepository.loadChatRoomMetadata(memberId.getValue());
    }

    @Data
    @AllArgsConstructor
    public static class MemberResponseRepositoryDto {
        private String memberState;
        private Provider provider;
        private LocalDate startLoveDate;
        private LoveTypeCategory loveTypeCategory;
        private float avoidanceRate;
        private float anxietyRate;
        private String nickname;
        private String email;
        private RelationshipStatus relationshipStatus;
        private String personalityType;
        private String otherPersonalityType;
        private PartnerLoveTypeCategory partnerLoveTypeCategory;

        public MemberQueryHelper.MemberInfoDto toDto(int totalChatRoomCount, int totalCoupleQuestionCount) {
            return MemberQueryHelper.MemberInfoDto.builder()
                    .memberState(memberState)
                    .provider(provider)
                    .startLoveDate(startLoveDate)
                    .loveTypeCategory(loveTypeCategory)
                    .avoidanceRate(avoidanceRate)
                    .anxietyRate(anxietyRate)
                    .nickname(nickname)
                    .email(email)
                    .totalChatRoomCount(totalChatRoomCount)
                    .totalCoupleQuestionCount(totalCoupleQuestionCount)
                    .relationshipStatus(relationshipStatus)
                    .personalityType(personalityType)
                    .otherPersonalityType(otherPersonalityType)
                    .partnerLoveTypeCategory(partnerLoveTypeCategory)
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    public static class PartnerMemberRepositoryDto {
        private String personalityType;
        private PartnerLoveTypeCategory loveTypeCategory;

        public MemberQueryHelper.PartnerMemberDto toDto() {
            return MemberQueryHelper.PartnerMemberDto.builder()
                    .personalityType(personalityType)
                    .loveTypeCategory(loveTypeCategory)
                    .description(loveTypeCategory == null ? null : loveTypeCategory.getDescription())
                    .build();
        }
    }
}
