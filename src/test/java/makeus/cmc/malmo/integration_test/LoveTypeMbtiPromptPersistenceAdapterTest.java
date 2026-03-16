package makeus.cmc.malmo.integration_test;

import jakarta.persistence.EntityManager;
import makeus.cmc.malmo.adaptor.out.persistence.entity.LoveTypeMbtiPromptEntity;
import makeus.cmc.malmo.application.port.out.LoadLoveTypeMbtiPromptPort;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeMbtiPrompt;
import makeus.cmc.malmo.domain.value.type.LoveTypeCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("LoveTypeMbtiPromptPersistenceAdapter 테스트")
class LoveTypeMbtiPromptPersistenceAdapterTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private LoadLoveTypeMbtiPromptPort loadLoveTypeMbtiPromptPort;

    @Test
    @DisplayName("MBTI 대소문자와 복합키 기준으로 프롬프트를 조회한다")
    void loadByMbtiAndLoveTypeCategory_findsPromptIgnoringMbtiCase() {
        // given
        em.persist(LoveTypeMbtiPromptEntity.builder()
                .mbti("ISTJ")
                .loveTypeCategory(LoveTypeCategory.STABLE_TYPE)
                .prompts("ISTJ 안정형 프롬프트")
                .build());
        em.flush();
        em.clear();

        // when
        LoveTypeMbtiPrompt prompt = loadLoveTypeMbtiPromptPort
                .loadByMbtiAndLoveTypeCategory("istj", LoveTypeCategory.STABLE_TYPE)
                .orElse(null);

        // then
        assertThat(prompt).isNotNull();
        assertThat(prompt.getMbti()).isEqualTo("ISTJ");
        assertThat(prompt.getLoveTypeCategory()).isEqualTo(LoveTypeCategory.STABLE_TYPE);
        assertThat(prompt.getPrompts()).isEqualTo("ISTJ 안정형 프롬프트");
    }
}
