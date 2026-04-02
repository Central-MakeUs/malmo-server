package makeus.cmc.malmo.application.helper.chat_room;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.application.exception.PromptNotFoundException;
import makeus.cmc.malmo.application.port.out.chat.LoadPromptPort;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import org.springframework.stereotype.Component;

import java.util.Optional;


@RequiredArgsConstructor
@Component
public class PromptQueryHelper {
    private final LoadPromptPort loadPromptPort;

    public Prompt getSystemPrompt() {
        return loadPromptPort.loadSystemPrompt()
                .orElseThrow(PromptNotFoundException::new);
    }


    public Prompt getGuidelinePrompt(int level) {
        return loadPromptPort.loadGuidelinePrompt(level)
                .orElseThrow(PromptNotFoundException::new);
    }

    /**
     * 프롬프트 조회 (fallback 지원)
     * 요청한 레벨의 프롬프트가 없으면 3단계 프롬프트 반환
     */
    public Prompt getGuidelinePromptWithFallback(int level) {
        Prompt prompt = loadPromptPort.loadGuidelinePrompt(level).orElse(null);
        
        if (prompt == null) {
            // 5단계 이상: 4단계 프롬프트 재사용
            return loadPromptPort.loadGuidelinePrompt(4)
                    .orElseThrow(PromptNotFoundException::new);
        }
        
        // isForCompletedResponse가 true인 경우도 무시하고 4단계 반환
        if (prompt.isForCompletedResponse()) {
            return loadPromptPort.loadGuidelinePrompt(4)
                    .orElseThrow(PromptNotFoundException::new);
        }
        
        return prompt;
    }

    public Prompt getAnswerMetadataPrompt() {
        return loadPromptPort.loadAnswerMetadataPrompt()
                .orElseThrow(PromptNotFoundException::new);
    }

    /**
     * 제목 생성 프롬프트 조회
     */
    public Prompt getTitleGenerationPrompt() {
        return loadPromptPort.loadTitleGenerationPrompt()
                .orElseThrow(() -> new RuntimeException("Title generation prompt not found"));
    }

    /**
     * 레벨별 요약 프롬프트 조회
     */
    public Optional<Prompt> getSummaryPromptByLevel(int level) {
        return loadPromptPort.loadSummaryPromptByLevel(level);
    }

    public Prompt getWeeklyReportPrompt() {
        return loadPromptPort.loadWeeklyReportPrompt()
                .orElseThrow(PromptNotFoundException::new);
    }
}
