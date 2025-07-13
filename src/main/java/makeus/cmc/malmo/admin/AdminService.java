package makeus.cmc.malmo.admin;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.jwt.JwtAdaptor;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.adaptor.out.persistence.entity.value.TermsEntityId;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.LoveTypeMapper;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.LoveTypeQuestionMapper;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberMapper;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.TermsMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.LoveTypeQuestionRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.LoveTypeRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.MemberTermsAgreementRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.TermsRepository;
import makeus.cmc.malmo.admin.exception.TermsAlreadyAgreedException;
import makeus.cmc.malmo.admin.exception.TermsAlreadyExistsException;
import makeus.cmc.malmo.domain.exception.LoveTypeNotFoundException;
import makeus.cmc.malmo.domain.exception.LoveTypeQuestionNotFoundException;
import makeus.cmc.malmo.domain.exception.MemberNotFoundException;
import makeus.cmc.malmo.domain.exception.TermsNotFoundException;
import makeus.cmc.malmo.domain.model.love_type.LoveType;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeQuestion;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.model.terms.Terms;
import makeus.cmc.malmo.domain.value.type.LoveTypeQuestionType;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import makeus.cmc.malmo.domain.value.type.TermsType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final MemberAdminRepository memberRepository;
    private final LoveTypeRepository loveTypeRepository;
    private final LoveTypeQuestionRepository loveTypeQuestionRepository;
    private final TermsRepository termsRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;

    private final MemberMapper memberMapper;
    private final LoveTypeMapper loveTypeMapper;
    private final LoveTypeQuestionMapper loveTypeQuestionMapper;
    private final TermsMapper termsMapper;

    private final JwtAdaptor jwtAdaptor;

    public List<Member> getMembers() {
        return memberRepository.findAll()
                .stream()
                .map(memberMapper::toDomain)
                .collect(Collectors.toList());
    }

    public TokenInfo login(String nickname, String token) {
        Member member = memberRepository.findMemberByNicknameAndFirebaseTokenAndMemberRole(nickname, token, MemberRole.ADMIN)
                .map(memberMapper::toDomain)
                .orElseThrow(MemberNotFoundException::new);

        return jwtAdaptor.generateToken(member.getId(), MemberRole.ADMIN);
    }

    public List<LoveType> getLoveTypes() {
        return loveTypeRepository.findAll()
                .stream()
                .map(loveTypeMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long updateLoveType(Long loveTypeId, String title, String summary, String content, String imageUrl) {
        LoveType loveType = loveTypeRepository.findById(loveTypeId)
                .map(loveTypeMapper::toDomain)
                .orElseThrow(LoveTypeNotFoundException::new);
        loveType.updateByAdmin(title, summary, content, imageUrl);
        loveTypeRepository.save(loveTypeMapper.toEntity(loveType));
        return loveType.getId();
    }

    public List<LoveTypeQuestion> getLoveTypeQuestions() {
        return loveTypeQuestionRepository.findAllByOrderByQuestionNumberAsc()
                .stream()
                .map(loveTypeQuestionMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long updateLoveTypeQuestion(Long questionId, String content, int questionNumber, boolean isReversed, LoveTypeQuestionType loveTypeQuestionType) {
        LoveTypeQuestion loveTypeQuestion = loveTypeQuestionRepository.findById(questionId)
                .map(loveTypeQuestionMapper::toDomain)
                .orElseThrow(LoveTypeQuestionNotFoundException::new);

        loveTypeQuestion.updateByAdmin(content, questionNumber, isReversed, loveTypeQuestionType);
        loveTypeQuestionRepository.save(loveTypeQuestionMapper.toEntity(loveTypeQuestion));
        return loveTypeQuestion.getId();
    }

    public List<Terms> getTerms() {
        return termsRepository.findAll()
                .stream()
                .map(termsMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long deleteTerms(Long termsId) {
        boolean isAlreadyAgreed = memberTermsAgreementRepository.existsByTermsEntityId(TermsEntityId.of(termsId));
        if (isAlreadyAgreed) {
            throw new TermsAlreadyAgreedException("Cannot delete terms that have already been agreed to by members.");
        }

        termsRepository.deleteById(termsId);
        return termsId;
    }

    @Transactional
    public Long createTerms(String title, String content, float version, boolean isRequired, TermsType termsType) {
        if (termsRepository.existsByTermsTypeAndVersion(termsType, version)) {
            throw new TermsAlreadyExistsException("Terms with the same type and version already exist.");
        }
        Terms terms = Terms.createTermsByAdmin(
                title,
                content,
                version,
                isRequired,
                termsType
        );
        termsRepository.save(termsMapper.toEntity(terms));
        return terms.getId();
    }


}

