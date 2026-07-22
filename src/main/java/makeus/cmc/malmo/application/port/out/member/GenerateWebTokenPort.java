package makeus.cmc.malmo.application.port.out.member;

import makeus.cmc.malmo.domain.value.type.MemberRole;

public interface GenerateWebTokenPort {

    String generateAccessToken(Long memberId, MemberRole memberRole);

    String generateRefreshToken();

    String hashRefreshToken(String refreshToken);
}
