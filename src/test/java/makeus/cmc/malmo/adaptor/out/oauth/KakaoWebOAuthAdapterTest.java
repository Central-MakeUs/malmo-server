package makeus.cmc.malmo.adaptor.out.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import makeus.cmc.malmo.application.port.out.member.WebOAuthProviderPort;
import makeus.cmc.malmo.config.WebSocialLoginProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Clock;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class KakaoWebOAuthAdapterTest {

    @Test
    void authorizationUriUsesPublicRestParametersWithoutSdkPkce() {
        WebSocialLoginProperties properties = new WebSocialLoginProperties();
        properties.getKakao().setClientId("rest-api-key");
        properties.getKakao().setRedirectUri("https://api.example.com/login/web/kakao/callback");

        KakaoWebOAuthAdapter adapter = new KakaoWebOAuthAdapter(
                properties, new ObjectMapper(), Clock.systemUTC(), new RestTemplate()
        );

        URI authorizationUri = adapter.authorizationUri(
                new WebOAuthProviderPort.AuthorizationRequest("state", "nonce")
        );

        assertThat(authorizationUri.toString())
                .contains("response_type=code", "state=state", "nonce=nonce")
                .doesNotContain("code_challenge", "code_challenge_method");
    }

    @Test
    void tokenResponseAcceptsStandardKakaoResponseFields() {
        String responseBody = """
                {
                  "token_type": "bearer",
                  "access_token": "access-token",
                  "expires_in": 43199,
                  "refresh_token": "refresh-token",
                  "refresh_token_expires_in": 5184000,
                  "scope": "openid account_email",
                  "id_token": "id-token"
                }
                """;
        Class<?> tokenResponseType = Arrays.stream(KakaoWebOAuthAdapter.class.getDeclaredClasses())
                .filter(type -> type.getSimpleName().equals("KakaoTokenResponse"))
                .findFirst()
                .orElseThrow();

        assertThatCode(() -> new ObjectMapper().readValue(responseBody, tokenResponseType))
                .doesNotThrowAnyException();
    }
}
