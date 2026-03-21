package makeus.cmc.malmo.config;

import makeus.cmc.malmo.application.port.out.chat.LlmReasoningScenario;
import makeus.cmc.malmo.config.properties.GeminiApiProperties;
import makeus.cmc.malmo.config.properties.OpenAiApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class LlmConfigurationPropertiesTest {

    @Autowired
    private OpenAiApiProperties openAiApiProperties;

    @Autowired
    private GeminiApiProperties geminiApiProperties;

    @Test
    void openAiProperties_areBoundFromConfiguration() {
        assertThat(openAiApiProperties.getKey()).isEqualTo("sk-test-openai-api-key-for-testing-only");
        assertThat(openAiApiProperties.getModel()).isEqualTo("gpt-5.4-mini");
        assertThat(openAiApiProperties.getBaseUrl()).isEqualTo("https://api.openai.com/v1");
        assertThat(openAiApiProperties.getStatusUrl()).isEqualTo("https://status.openai.com/api/v2/status.json");
        assertThat(openAiApiProperties.getReasoningEffort().getDefault()).isEqualTo("medium");
        assertThat(openAiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.STRUCTURED_CHAT)).isEqualTo("low");
        assertThat(openAiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.FREE_CONVERSATION)).isEqualTo("low");
        assertThat(openAiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.VALIDATION)).isEqualTo("none");
        assertThat(openAiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.SUMMARY)).isEqualTo("none");
        assertThat(openAiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.AUXILIARY_EXTRACTION)).isEqualTo("none");
    }

    @Test
    void geminiProperties_areBoundFromConfiguration() {
        assertThat(geminiApiProperties.getKey()).isEqualTo("test-gemini-api-key-for-testing-only");
        assertThat(geminiApiProperties.getModel()).isEqualTo("gemini-3-flash-preview");
        assertThat(geminiApiProperties.getBaseUrl())
                .isEqualTo("https://generativelanguage.googleapis.com/v1beta/openai");
        assertThat(geminiApiProperties.getReasoningEffort().getDefault()).isEqualTo("high");
        assertThat(geminiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.STRUCTURED_CHAT)).isEqualTo("medium");
        assertThat(geminiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.FREE_CONVERSATION)).isEqualTo("low");
        assertThat(geminiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.VALIDATION)).isEqualTo("low");
        assertThat(geminiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.SUMMARY)).isEqualTo("low");
        assertThat(geminiApiProperties.getReasoningEffort().getScenarioEffort(LlmReasoningScenario.AUXILIARY_EXTRACTION)).isEqualTo("low");
    }
}
