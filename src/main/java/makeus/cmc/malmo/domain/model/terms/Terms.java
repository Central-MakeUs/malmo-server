package makeus.cmc.malmo.domain.model.terms;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.type.TermsType;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class Terms {
    private final Long id;
    private final String title;
    private final String content;
    private final float version;
    private final boolean isRequired;
    private final TermsType termsType;

    // BaseTimeEntity fields
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private LocalDateTime deletedAt;

    public static Terms from(Long id, String title, String content, float version, boolean isRequired,
                             TermsType termsType, LocalDateTime createdAt, LocalDateTime modifiedAt,
                             LocalDateTime deletedAt) {
        return Terms.builder()
                .id(id)
                .title(title)
                .content(content)
                .version(version)
                .isRequired(isRequired)
                .termsType(termsType)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public static Terms createTermsByAdmin(String title, String content, float version, boolean isRequired, TermsType termsType) {
        return Terms.builder()
                .title(title)
                .content(content)
                .version(version)
                .isRequired(isRequired)
                .termsType(termsType)
                .build();
    }
}