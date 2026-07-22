package makeus.cmc.malmo.adaptor.out.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import makeus.cmc.malmo.adaptor.out.exception.RestApiException;
import makeus.cmc.malmo.adaptor.out.oidc.WebOidcTokenValidator;
import makeus.cmc.malmo.application.port.out.member.WebOAuthProviderPort;
import makeus.cmc.malmo.config.WebSocialLoginProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Clock;

@Component
public class KakaoWebOAuthAdapter implements WebOAuthProviderPort {

    private final WebSocialLoginProperties.Kakao properties;
    private final ObjectMapper objectMapper;
    private final WebOidcTokenValidator tokenValidator;
    private final RestTemplate restTemplate;

    public KakaoWebOAuthAdapter(
            WebSocialLoginProperties properties,
            ObjectMapper objectMapper,
            Clock clock,
            @Qualifier("webOAuthRestTemplate") RestTemplate restTemplate
    ) {
        this.properties = properties.getKakao();
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.tokenValidator = new WebOidcTokenValidator(
                this.properties.getIssuer(),
                this.properties.getClientId(),
                this.properties.getJwksUri(),
                clock
        );
    }

    @Override
    public URI authorizationUri(AuthorizationRequest request) {
        return UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("scope", "openid,account_email")
                .queryParam("state", request.state())
                .queryParam("nonce", request.nonce())
                .build()
                .encode()
                .toUri();
    }

    @Override
    public Identity exchange(AuthorizationCode authorizationCode) {
        KakaoTokenResponse tokenResponse = requestTokens(authorizationCode);
        if (tokenResponse == null || tokenResponse.idToken() == null || tokenResponse.accessToken() == null) {
            throw new RestApiException("카카오 웹 로그인 토큰 응답이 올바르지 않습니다.");
        }
        WebOidcTokenValidator.Claims claims = tokenValidator.validate(
                tokenResponse.idToken(), authorizationCode.nonce()
        );
        KakaoUserInfo userInfo = requestUserInfo(tokenResponse.accessToken());
        if (!claims.subject().equals(userInfo.subject())) {
            throw new RestApiException("카카오 ID Token과 사용자 정보의 주체가 일치하지 않습니다.");
        }
        return new Identity(claims.subject(), userInfo.email());
    }

    private KakaoTokenResponse requestTokens(AuthorizationCode authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", properties.getClientId());
        body.add("redirect_uri", properties.getRedirectUri());
        body.add("code", authorizationCode.code());
        if (!properties.getClientSecret().isBlank()) {
            body.add("client_secret", properties.getClientSecret());
        }
        try {
            return restTemplate.postForObject(
                    properties.getTokenUri(), new HttpEntity<>(body, headers), KakaoTokenResponse.class
            );
        } catch (RestClientException e) {
            throw new RestApiException("카카오 웹 authorization code 교환에 실패했습니다.", e);
        }
    }

    private KakaoUserInfo requestUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getUserInfoUri(), HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            JsonNode json = objectMapper.readTree(response.getBody());
            String subject = json.path("sub").asText(null);
            String email = json.path("email_verified").asBoolean(false)
                    ? json.path("email").asText(null)
                    : null;
            if (subject == null) {
                throw new RestApiException("카카오 웹 사용자 정보에 sub가 없습니다.");
            }
            return new KakaoUserInfo(subject, email);
        } catch (RestApiException e) {
            throw e;
        } catch (Exception e) {
            throw new RestApiException("카카오 웹 사용자 정보 조회에 실패했습니다.", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("id_token") String idToken
    ) {
    }

    private record KakaoUserInfo(String subject, String email) {
    }
}
