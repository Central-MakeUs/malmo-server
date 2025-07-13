package makeus.cmc.malmo.admin;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.jwt.JwtAdaptor;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.MemberMapper;
import makeus.cmc.malmo.domain.exception.MemberNotFoundException;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.type.MemberRole;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final MemberAdminRepository memberRepository;
    private final MemberMapper memberMapper;
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

}
