package makeus.cmc.malmo.application.service.member;

import makeus.cmc.malmo.application.exception.InvalidWebOAuthRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebReturnUrlValidatorTest {

    private final WebReturnUrlValidator validator =
            new WebReturnUrlValidator(List.of("https://web.malmo.example", "http://localhost:3000"));

    @Test
    void allowsPathOnConfiguredOrigin() {
        assertThatCode(() -> validator.validate("https://web.malmo.example/auth/callback?from=login"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnconfiguredOriginAndLookalikeHost() {
        assertThatThrownBy(() -> validator.validate("https://web.malmo.example.attacker.com/callback"))
                .isInstanceOf(InvalidWebOAuthRequestException.class);
        assertThatThrownBy(() -> validator.validate("https://attacker.example/callback"))
                .isInstanceOf(InvalidWebOAuthRequestException.class);
    }
}
