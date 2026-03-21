package makeus.cmc.malmo.config;

import makeus.cmc.malmo.adaptor.out.OpenAiApiClient;
import makeus.cmc.malmo.application.port.out.CheckLlmHealth;
import makeus.cmc.malmo.application.port.out.chat.RequestChatApiPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
class LlmClientSelectionTest {

    @Autowired
    private RequestChatApiPort requestChatApiPort;

    @Autowired
    private CheckLlmHealth checkLlmHealth;

    @Test
    void primaryLlmClient_isOpenAi() {
        assertThat(requestChatApiPort).isInstanceOf(OpenAiApiClient.class);
        assertThat(checkLlmHealth).isInstanceOf(OpenAiApiClient.class);
    }
}
