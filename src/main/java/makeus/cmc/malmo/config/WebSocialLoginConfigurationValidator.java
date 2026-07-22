package makeus.cmc.malmo.config;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.service.member.WebReturnUrlValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocialLoginConfigurationValidator implements InitializingBean {

    private final WebSocialLoginProperties properties;

    @Override
    public void afterPropertiesSet() {
        new WebReturnUrlValidator(properties.getAllowedReturnOrigins());
        if (properties.getAllowedReturnOrigins().stream().noneMatch(value -> value != null && !value.isBlank())) {
            throw new IllegalStateException("웹 소셜 로그인 허용 return origin이 필요합니다.");
        }
        requireNonBlank("KAKAO_WEB_REST_API_KEY", properties.getKakao().getClientId());
        requireNonBlank("KAKAO_WEB_REDIRECT_URI", properties.getKakao().getRedirectUri());
    }

    private static void requireNonBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 설정이 필요합니다.");
        }
    }
}
