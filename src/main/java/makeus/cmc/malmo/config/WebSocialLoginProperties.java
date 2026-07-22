package makeus.cmc.malmo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "social-login.web")
public class WebSocialLoginProperties {

    private List<String> allowedReturnOrigins = new ArrayList<>();
    private long stateExpirationSeconds = 300;
    private long ticketExpirationSeconds = 60;
    private long refreshTokenExpirationSeconds = 2_592_000;
    private Kakao kakao = new Kakao();

    @Getter
    @Setter
    public static class Kakao {
        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";
        private String authorizationUri = "https://kauth.kakao.com/oauth/authorize";
        private String tokenUri = "https://kauth.kakao.com/oauth/token";
        private String userInfoUri = "https://kapi.kakao.com/v1/oidc/userinfo";
        private String issuer = "https://kauth.kakao.com";
        private String jwksUri = "https://kauth.kakao.com/.well-known/jwks.json";
    }
}
