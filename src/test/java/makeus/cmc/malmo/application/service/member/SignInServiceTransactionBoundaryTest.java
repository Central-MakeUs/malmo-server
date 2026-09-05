package makeus.cmc.malmo.application.service.member;

import makeus.cmc.malmo.application.port.in.member.SignInUseCase.SignInAppleCommand;
import makeus.cmc.malmo.application.port.in.member.SignInUseCase.SignInKakaoCommand;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SignInServiceTransactionBoundaryTest {

    private final AnnotationTransactionAttributeSource transactionAttributeSource =
            new AnnotationTransactionAttributeSource();

    @Test
    void mobileSignInDoesNotOpenAnOuterTransaction() throws NoSuchMethodException {
        Method kakaoSignIn = SignInService.class.getMethod("signInKakao", SignInKakaoCommand.class);
        Method appleSignIn = SignInService.class.getMethod("signInApple", SignInAppleCommand.class);

        assertThat(transactionAttributeSource.getTransactionAttribute(kakaoSignIn, SignInService.class))
                .isNull();
        assertThat(transactionAttributeSource.getTransactionAttribute(appleSignIn, SignInService.class))
                .isNull();
    }
}
