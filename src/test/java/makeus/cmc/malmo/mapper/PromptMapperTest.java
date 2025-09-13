package makeus.cmc.malmo.mapper;

import makeus.cmc.malmo.adaptor.out.persistence.entity.chat.PromptEntity;
import makeus.cmc.malmo.adaptor.out.persistence.mapper.PromptMapper;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromptMapper 테스트")
class PromptMapperTest {

    @InjectMocks
    private PromptMapper promptMapper;

    @Test
    @DisplayName("Entity를 Domain으로 변환한다")
    void toDomain() {
        // given
        LocalDateTime now = LocalDateTime.now();
        PromptEntity entity = PromptEntity.builder()
                .id(1L)
                .level(1)
                .content("prompt content")
                .createdAt(now)
                .modifiedAt(now)
                .deletedAt(null)
                .build();

        // when
        Prompt domain = promptMapper.toDomain(entity);

        // then
        assertThat(domain.getId()).isEqualTo(entity.getId());
        assertThat(domain.getLevel()).isEqualTo(entity.getLevel());
        assertThat(domain.getContent()).isEqualTo(entity.getContent());
        assertThat(domain.getCreatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(domain.getModifiedAt()).isEqualTo(entity.getModifiedAt());
        assertThat(domain.getDeletedAt()).isEqualTo(entity.getDeletedAt());
    }

    @Test
    @DisplayName("Domain을 Entity로 변환한다")
    void toEntity() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Prompt domain = Prompt.from(
                1L,
                1,
                "prompt content",
                now,
                now,
                null
        );

        // when
        PromptEntity entity = promptMapper.toEntity(domain);

        // then
        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getLevel()).isEqualTo(domain.getLevel());
        assertThat(entity.getContent()).isEqualTo(domain.getContent());
        assertThat(entity.getCreatedAt()).isEqualTo(domain.getCreatedAt());
        assertThat(entity.getModifiedAt()).isEqualTo(domain.getModifiedAt());
        assertThat(entity.getDeletedAt()).isEqualTo(domain.getDeletedAt());
    }

    @Test
    @DisplayName("null Entity를 Domain으로 변환하면 null을 반환한다")
    void toDomain_withNullEntity() {
        // when
        Prompt domain = promptMapper.toDomain(null);

        // then
        assertThat(domain).isNull();
    }

    @Test
    @DisplayName("null Domain을 Entity로 변환하면 null을 반환한다")
    void toEntity_withNullDomain() {
        // when
        PromptEntity entity = promptMapper.toEntity(null);

        // then
        assertThat(entity).isNull();
    }
}