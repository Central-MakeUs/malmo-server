package makeus.cmc.malmo.application.port.in.member;

import java.net.URI;

public interface WebSignInUseCase {

    URI startAuthorization(String returnUrl, String deviceId);

    URI completeAuthorization(String code, String state);

    URI failAuthorization(String state, String error);

    SignInResponse exchangeTicket(String ticket);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);

    record SignInResponse(
            String memberState,
            String grantType,
            String accessToken,
            String refreshToken
    ) {
    }

    record TokenResponse(
            String grantType,
            String accessToken,
            String refreshToken
    ) {
    }
}
