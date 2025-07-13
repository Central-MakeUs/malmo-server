package makeus.cmc.malmo.admin;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.jwt.JwtAdaptor;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.LoveTypeMapper;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.LoveTypeQuestionMapper;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberMapper;
import makeus.cmc.malmo.adaptor.out.persistence.repository.LoveTypeQuestionRepository;
import makeus.cmc.malmo.adaptor.out.persistence.repository.LoveTypeRepository;
import makeus.cmc.malmo.domain.exception.LoveTypeNotFoundException;
import makeus.cmc.malmo.domain.exception.LoveTypeQuestionNotFoundException;
import makeus.cmc.malmo.domain.exception.MemberNotFoundException;
import makeus.cmc.malmo.domain.model.love_type.LoveType;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeQuestion;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.type.LoveTypeQuestionType;
import makeus.cmc.malmo.domain.value.type.MemberRole;
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
    private final MemberMapper memberMapper;
    private final LoveTypeMapper loveTypeMapper;
    private final LoveTypeQuestionMapper loveTypeQuestionMapper;
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
}

