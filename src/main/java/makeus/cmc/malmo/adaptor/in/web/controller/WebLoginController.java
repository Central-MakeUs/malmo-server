package makeus.cmc.malmo.adaptor.in.web.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseResponse;
import makeus.cmc.malmo.application.exception.InvalidWebOAuthRequestException;
import makeus.cmc.malmo.application.port.in.member.WebSignInUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class WebLoginController {

    private final WebSignInUseCase webSignInUseCase;

    @GetMapping("/login/web/kakao/authorize")
    public ResponseEntity<Void> authorizeKakao(
            @RequestParam String returnUrl,
            @RequestParam(required = false) String deviceId
    ) {
        return redirect(webSignInUseCase.startAuthorization(returnUrl, deviceId));
    }

    @GetMapping("/login/web/kakao/callback")
    public ResponseEntity<Void> callbackKakao(
            @RequestParam String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error
    ) {
        return callback(state, code, error);
    }

    @PostMapping("/login/web/exchange")
    public BaseResponse<WebSignInUseCase.SignInResponse> exchange(
            @Valid @RequestBody TicketRequest request
    ) {
        return BaseResponse.success(webSignInUseCase.exchangeTicket(request.ticket()));
    }

    @PostMapping("/login/web/refresh")
    public BaseResponse<WebSignInUseCase.TokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return BaseResponse.success(webSignInUseCase.refresh(request.refreshToken()));
    }

    @PostMapping("/login/web/logout")
    public BaseResponse<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        webSignInUseCase.logout(request.refreshToken());
        return BaseResponse.success(null);
    }

    private ResponseEntity<Void> callback(
            String state,
            String code,
            String error
    ) {
        URI location;
        if (error != null && !error.isBlank()) {
            location = webSignInUseCase.failAuthorization(state, error);
        } else {
            if (code == null || code.isBlank()) {
                throw new InvalidWebOAuthRequestException("authorization code가 필요합니다.");
            }
            location = webSignInUseCase.completeAuthorization(code, state);
        }
        return redirect(location);
    }

    private static ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString())
                .cacheControl(CacheControl.noStore())
                .build();
    }

    public record TicketRequest(@NotBlank String ticket) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }
}
