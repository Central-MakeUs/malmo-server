package makeus.cmc.malmo.adaptor.in.exception;

import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.out.exception.SseConnectionException;
import makeus.cmc.malmo.adaptor.out.oidc.exception.OidcIdTokenException;
import makeus.cmc.malmo.adaptor.out.oidc.exception.RestApiException;
import makeus.cmc.malmo.admin.exception.TermsAlreadyAgreedException;
import makeus.cmc.malmo.admin.exception.TermsAlreadyExistsException;
import makeus.cmc.malmo.application.exception.InvalidRefreshTokenException;
import makeus.cmc.malmo.domain.exception.*;
import org.hibernate.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MemberNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleMemberNotFoundException(MemberNotFoundException e) {
        log.error("[GlobalExceptionHandler: handleMemberNotFoundException 호출]", e);
        return ErrorResponse.of(ErrorCode.NO_SUCH_MEMBER);
    }

    @ExceptionHandler({InviteCodeNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleCoupleCodeNotFoundException(InviteCodeNotFoundException e) {
        log.error("[GlobalExceptionHandler: handleCoupleCodeNotFoundException 호출]", e);
        return ErrorResponse.of(ErrorCode.NO_SUCH_COUPLE_CODE);
    }

    @ExceptionHandler({OidcIdTokenException.class})
    public ResponseEntity<ErrorResponse> handleOidcIdTokenException(OidcIdTokenException e) {
        log.error("[GlobalExceptionHandler: handleOidcIdTokenException 호출]", e);
        return ErrorResponse.of(ErrorCode.INVALID_ID_TOKEN);
    }

    @ExceptionHandler({RestApiException.class})
    public ResponseEntity<ErrorResponse> handleRestApiException(RestApiException e) {
        log.error("[GlobalExceptionHandler: handleRestApiException 호출]", e);
        return ErrorResponse.of(ErrorCode.EXTERNAL_API_ERROR);
    }

    @ExceptionHandler({InvalidRefreshTokenException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRefreshTokenException(InvalidRefreshTokenException e) {
        log.error("[GlobalExceptionHandler: handleInvalidRefreshTokenException 호출]", e);
        return ErrorResponse.of(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @ExceptionHandler({InviteCodeGenerateFailedException.class})
    public ResponseEntity<ErrorResponse> handleInviteCodeGenerateFailedException(InviteCodeGenerateFailedException e) {
        log.error("[GlobalExceptionHandler: handleInviteCodeGenerateFailedException 호출]", e);
        return ErrorResponse.of(ErrorCode.COUPLE_CODE_GENERATE_FAILED);
    }

    @ExceptionHandler({TermsNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleTermsNotFoundException(TermsNotFoundException e) {
        log.error("[GlobalExceptionHandler: handleTermsNotFoundException 호출]", e);
        return ErrorResponse.of(ErrorCode.NO_SUCH_TERMS);
    }

    @ExceptionHandler({LoveTypeNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleLoveTypeNotFoundException(LoveTypeNotFoundException e) {
        log.error("[GlobalExceptionHandler: handleLoveTypeNotFoundException 호출]", e);
        return ErrorResponse.of(ErrorCode.NO_SUCH_LOVE_TYPE);
    }

    @ExceptionHandler({LoveTypeQuestionNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleLoveTypeQuestionNotFoundException(LoveTypeQuestionNotFoundException e) {
        log.error("[GlobalExceptionHandler: handleLoveTypeQuestionNotFoundException 호출]", e);
        return ErrorResponse.of(ErrorCode.NO_SUCH_LOVE_TYPE_QUESTION);
    }

    @ExceptionHandler({NotCoupleMemberException.class})
    public ResponseEntity<ErrorResponse> handleNotCoupleMemberException(NotCoupleMemberException e) {
        log.error("[GlobalExceptionHandler: handleNotCoupleMemberException 호출]", e);
        return ErrorResponse.of(ErrorCode.NOT_COUPLE_MEMBER);
    }

    @ExceptionHandler({UsedInviteCodeException.class})
    public ResponseEntity<ErrorResponse> handleUsedCoupleCodeException(UsedInviteCodeException e) {
        log.error("[GlobalExceptionHandler: handleUsedCoupleCodeException 호출]", e);
        return ErrorResponse.of(ErrorCode.USED_COUPLE_CODE);
    }

    @ExceptionHandler({AlreadyCoupledMemberException.class})
    public ResponseEntity<ErrorResponse> handleAlreadyCoupledMemberException(AlreadyCoupledMemberException e) {
        log.error("[GlobalExceptionHandler: handleAlreadyCoupledMemberException 호출]", e);
        return ErrorResponse.of(ErrorCode.ALREADY_COUPLED_MEMBER);
    }

    @ExceptionHandler({SseConnectionException.class})
    public ResponseEntity<ErrorResponse> handleSseConnectionException(SseConnectionException e) {
        log.error("[GlobalExceptionHandler: handleSseConnectionException 호출]", e);
        return ErrorResponse.of(ErrorCode.SSE_CONNECTION_ERROR);
    }

    @ExceptionHandler({TermsAlreadyAgreedException.class})
    public ResponseEntity<ErrorResponse> handleTermsAlreadyAgreedException(TermsAlreadyAgreedException e) {
        log.error("[GlobalExceptionHandler: handleTermsAlreadyAgreedException 호출]", e);
        return ErrorResponse.of(ErrorCode.TERMS_ALREADY_AGREED);
    }

    @ExceptionHandler({TermsAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handleTermsAlreadyExistsException(TermsAlreadyExistsException e) {
        log.error("[GlobalExceptionHandler: handleTermsAlreadyExistsException 호출]", e);
        return ErrorResponse.of(ErrorCode.TERMS_ALREADY_EXISTS);
    }





    /**
     *  ---------- 공통 예외 처리 핸들러 ----------
      */
    @ExceptionHandler({NoHandlerFoundException.class, TypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestException(Exception e) {
        log.error("[CommonExceptionHandler: handleBadRequestException 호출]", e);
        return ErrorResponse.of(ErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("[CommonExceptionHandler: handleHttpRequestMethodNotSupportedException 호출]", e);
        return ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler({IllegalArgumentException.class, IOException.class})
    public ResponseEntity<ErrorResponse> handleInternalServerException(Exception e) {
        log.error("[CommonExceptionHandler: handleInternalServerException 호출]", e);
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(Exception e) {
        log.error("[CommonExceptionHandler: handleRuntimeException 호출]", e);
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("[CommonExceptionHandler: handleMethodArgumentNotValidException 호출]", e);
        return ErrorResponse.of(ErrorCode.BAD_REQUEST);
    }
}
