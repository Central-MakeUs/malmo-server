package makeus.cmc.malmo.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class WebSocialLoginConfig {

    @Bean
    public Clock webSocialLoginClock() {
        return Clock.systemUTC();
    }

    @Bean("webOAuthRestTemplate")
    public RestTemplate webOAuthRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
