package makeus.cmc.malmo.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini.api")
public class GeminiApiProperties {

    private String key;
    private String model;
    private String baseUrl;
    private ReasoningEffortProperties reasoningEffort = new ReasoningEffortProperties();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ReasoningEffortProperties getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(ReasoningEffortProperties reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }
}
