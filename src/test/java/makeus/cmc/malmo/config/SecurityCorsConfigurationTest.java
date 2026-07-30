package makeus.cmc.malmo.config;

import makeus.cmc.malmo.adaptor.in.web.security.CustomAccessDeniedHandler;
import makeus.cmc.malmo.adaptor.in.web.security.CustomAuthenticationEntryPoint;
import makeus.cmc.malmo.adaptor.out.jwt.JwtAdaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityCorsConfigurationTest {

    private static final String PRODUCTION_SERVER_URL = "https://api.malmo.io.kr";
    private static final String DEVELOPMENT_SERVER_URL = "https://test.malmo.io.kr";
    private static final String PRODUCTION_CLIENT_URL = "https://malmo.io.kr";
    private static final String DEVELOPMENT_CLIENT_URL = "https://dev.malmo.io.kr";
    private static final String WWW_CLIENT_URL = "https://www.malmo.io.kr";

    @Mock
    private JwtAdaptor jwtAdaptor;

    @Mock
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void productionCorsIncludesWebReturnOriginsWithoutDuplicates() {
        WebSocialLoginProperties properties = webSocialLoginProperties();
        SecurityConfig securityConfig = new SecurityConfig(
                jwtAdaptor, authenticationEntryPoint, accessDeniedHandler, properties
        );
        setConfiguredOrigins(securityConfig);

        assertAllowedOrigins(securityConfig.corsConfigurationSource());
    }

    @Test
    void nonProductionCorsIncludesWebReturnOriginsWithoutDuplicates() {
        WebSocialLoginProperties properties = webSocialLoginProperties();
        TestSecurityConfig securityConfig = new TestSecurityConfig(
                jwtAdaptor, authenticationEntryPoint, accessDeniedHandler, properties
        );
        setConfiguredOrigins(securityConfig);

        assertAllowedOrigins(securityConfig.corsConfigurationSource());
    }

    private static WebSocialLoginProperties webSocialLoginProperties() {
        WebSocialLoginProperties properties = new WebSocialLoginProperties();
        properties.setAllowedReturnOrigins(List.of(PRODUCTION_CLIENT_URL, WWW_CLIENT_URL));
        return properties;
    }

    private static void setConfiguredOrigins(Object securityConfig) {
        ReflectionTestUtils.setField(securityConfig, "PRODUCTION_SERVER_URL", PRODUCTION_SERVER_URL);
        ReflectionTestUtils.setField(securityConfig, "DEVELOPMENT_SERVER_URL", DEVELOPMENT_SERVER_URL);
        ReflectionTestUtils.setField(securityConfig, "PRODUCTION_CLIENT_URL", PRODUCTION_CLIENT_URL);
        ReflectionTestUtils.setField(securityConfig, "DEVELOPMENT_CLIENT_URL", DEVELOPMENT_CLIENT_URL);
    }

    private static void assertAllowedOrigins(CorsConfigurationSource source) {
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .contains(PRODUCTION_CLIENT_URL, WWW_CLIENT_URL)
                .doesNotHaveDuplicates();
    }
}
