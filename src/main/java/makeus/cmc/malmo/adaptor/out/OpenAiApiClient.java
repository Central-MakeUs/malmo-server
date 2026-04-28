package makeus.cmc.malmo.adaptor.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.out.CheckLlmHealth;
import makeus.cmc.malmo.config.properties.OpenAiApiProperties;
import makeus.cmc.malmo.config.properties.ReasoningEffortProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
//@Primary
@Component
public class OpenAiApiClient extends AbstractOpenAiCompatibleApiClient implements CheckLlmHealth {

    private final OpenAiApiProperties openAiApiProperties;
    private final RestTemplate restTemplate;

    public OpenAiApiClient(
            @Qualifier("openAiWebClient") WebClient webClient,
            ObjectMapper objectMapper,
            OpenAiApiProperties openAiApiProperties,
            RestTemplate restTemplate
    ) {
        super(webClient, objectMapper);
        this.openAiApiProperties = openAiApiProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    protected String getProviderName() {
        return "OpenAI";
    }

    @Override
    protected String getApiKey() {
        return openAiApiProperties.getKey();
    }

    @Override
    protected String getModel() {
        return openAiApiProperties.getModel();
    }

    @Override
    protected ReasoningEffortProperties getReasoningEffortProperties() {
        return openAiApiProperties.getReasoningEffort();
    }

    @Override
    public boolean checkHealth() {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    openAiApiProperties.getStatusUrl(),
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    Map.class
            );

            Map body = response.getBody();
            if (body == null) {
                log.warn("OpenAI HealthCheck: Empty response");
                return false;
            }

            Map status = (Map) body.get("status");
            String indicator = (String) status.get("indicator");
            String description = (String) status.get("description");

            if ("none".equalsIgnoreCase(indicator)) {
                log.info("OpenAI API is UP: {}", description);
                return true;
            }

            log.warn("OpenAI API Issue: {} ({})", description, indicator);
            return false;
        } catch (Exception e) {
            log.error("OpenAI HealthCheck failed", e);
            return false;
        }
    }
}
