package makeus.cmc.malmo.adaptor.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.out.CheckLlmHealth;
import makeus.cmc.malmo.config.properties.GeminiApiProperties;
import makeus.cmc.malmo.config.properties.ReasoningEffortProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
//@Primary
@Component
public class GeminiApiClient extends AbstractOpenAiCompatibleApiClient implements CheckLlmHealth {

    private final GeminiApiProperties geminiApiProperties;
    private final RestTemplate restTemplate;

    public GeminiApiClient(
            @Qualifier("geminiWebClient") WebClient webClient,
            ObjectMapper objectMapper,
            GeminiApiProperties geminiApiProperties,
            RestTemplate restTemplate
    ) {
        super(webClient, objectMapper);
        this.geminiApiProperties = geminiApiProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    protected String getProviderName() {
        return "Gemini";
    }

    @Override
    protected String getApiKey() {
        return geminiApiProperties.getKey();
    }

    @Override
    protected String getModel() {
        return geminiApiProperties.getModel();
    }

    @Override
    protected ReasoningEffortProperties getReasoningEffortProperties() {
        return geminiApiProperties.getReasoningEffort();
    }

    @Override
    public boolean checkHealth() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(geminiApiProperties.getKey());

            ResponseEntity<String> response = restTemplate.exchange(
                    geminiApiProperties.getBaseUrl() + "/models/" + geminiApiProperties.getModel(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            boolean isUp = response.getStatusCode().is2xxSuccessful();
            if (isUp) {
                log.info("Gemini API is UP for model={}", geminiApiProperties.getModel());
            } else {
                log.warn("Gemini API check returned status={} for model={}",
                        response.getStatusCode(), geminiApiProperties.getModel());
            }
            return isUp;
        } catch (Exception e) {
            log.error("Gemini HealthCheck failed", e);
            return false;
        }
    }
}
