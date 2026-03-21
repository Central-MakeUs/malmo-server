package makeus.cmc.malmo.adaptor.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import makeus.cmc.malmo.application.port.out.chat.LlmReasoningScenario;
import makeus.cmc.malmo.config.properties.ReasoningEffortProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractOpenAiCompatibleApiClientTest {

    @Test
    void createBody_usesScenarioSpecificReasoningEffort_whenPresent() {
        TestClient client = new TestClient(reasoningEffortProperties("medium", Map.of(
                LlmReasoningScenario.VALIDATION, "none"
        )));

        Map<String, Object> body = client.exposeCreateBody(LlmReasoningScenario.VALIDATION);

        assertThat(body.get("reasoning_effort")).isEqualTo("none");
    }

    @Test
    void createBody_fallsBackToDefaultReasoningEffort_whenScenarioOverrideMissing() {
        TestClient client = new TestClient(reasoningEffortProperties("medium", Map.of()));

        Map<String, Object> body = client.exposeCreateBody(LlmReasoningScenario.STRUCTURED_CHAT);

        assertThat(body.get("reasoning_effort")).isEqualTo("medium");
    }

    @Test
    void createBody_omitsReasoningEffort_whenNoValuesConfigured() {
        TestClient client = new TestClient(reasoningEffortProperties(null, Map.of()));

        Map<String, Object> body = client.exposeCreateBody(LlmReasoningScenario.SUMMARY);

        assertThat(body).doesNotContainKey("reasoning_effort");
    }

    private static ReasoningEffortProperties reasoningEffortProperties(String defaultEffort,
                                                                       Map<LlmReasoningScenario, String> scenarios) {
        ReasoningEffortProperties properties = new ReasoningEffortProperties();
        properties.setDefault(defaultEffort);
        properties.setScenarios(scenarios);
        return properties;
    }

    private static final class TestClient extends AbstractOpenAiCompatibleApiClient {
        private final ReasoningEffortProperties reasoningEffortProperties;

        private TestClient(ReasoningEffortProperties reasoningEffortProperties) {
            super(WebClient.builder().baseUrl("https://example.com").build(), new ObjectMapper());
            this.reasoningEffortProperties = reasoningEffortProperties;
        }

        private Map<String, Object> exposeCreateBody(LlmReasoningScenario scenario) {
            return createBody(List.of(Map.of("role", "user", "content", "hello")), scenario, false, false);
        }

        @Override
        protected String getProviderName() {
            return "Test";
        }

        @Override
        protected String getApiKey() {
            return "test-key";
        }

        @Override
        protected String getModel() {
            return "test-model";
        }

        @Override
        protected ReasoningEffortProperties getReasoningEffortProperties() {
            return reasoningEffortProperties;
        }
    }
}
