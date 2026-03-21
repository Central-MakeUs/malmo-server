package makeus.cmc.malmo.config.properties;

import makeus.cmc.malmo.application.port.out.chat.LlmReasoningScenario;

import java.util.EnumMap;
import java.util.Map;

public class ReasoningEffortProperties {

    private String defaultEffort;
    private Map<LlmReasoningScenario, String> scenarios = new EnumMap<>(LlmReasoningScenario.class);

    public String getDefault() {
        return defaultEffort;
    }

    public void setDefault(String defaultEffort) {
        this.defaultEffort = defaultEffort;
    }

    public Map<LlmReasoningScenario, String> getScenarios() {
        return scenarios;
    }

    public void setScenarios(Map<LlmReasoningScenario, String> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            this.scenarios = new EnumMap<>(LlmReasoningScenario.class);
            return;
        }
        this.scenarios = new EnumMap<>(scenarios);
    }

    public String getScenarioEffort(LlmReasoningScenario scenario) {
        return scenarios.get(scenario);
    }
}
