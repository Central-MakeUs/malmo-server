package makeus.cmc.malmo.adaptor.in.exception;

import io.sentry.Sentry;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.adaptor.out.exception.OidcIdTokenException;
import makeus.cmc.malmo.adaptor.out.exception.RestApiException;
import makeus.cmc.malmo.adaptor.out.exception.SseConnectionException;
import makeus.cmc.malmo.application.exception.*;
import org.hibernate.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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
        log.warn("[GlobalExceptionHandler: handleMemberNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_MEMBER);
    }

    @ExceptionHandler({InviteCodeNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleCoupleCodeNotFoundException(InviteCodeNotFoundException e) {
        log.info("[GlobalExceptionHandler: handleCoupleCodeNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_COUPLE_CODE);
    }

    @ExceptionHandler({OidcIdTokenException.class})
    public ResponseEntity<ErrorResponse> handleOidcIdTokenException(OidcIdTokenException e) {
        log.warn("[GlobalExceptionHandler: handleOidcIdTokenException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.INVALID_ID_TOKEN);
    }

    @ExceptionHandler({RestApiException.class})
    public ResponseEntity<ErrorResponse> handleRestApiException(RestApiException e) {
        log.error("[GlobalExceptionHandler: handleRestApiException 호출]", e);
        Sentry.captureException(e);
        return ErrorResponse.of(ErrorCode.EXTERNAL_API_ERROR);
    }

    @ExceptionHandler({InvalidRefreshTokenException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRefreshTokenException(InvalidRefreshTokenException e) {
        log.info("[GlobalExceptionHandler: handleInvalidRefreshTokenException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @ExceptionHandler({InviteCodeGenerateFailedException.class})
    public ResponseEntity<ErrorResponse> handleInviteCodeGenerateFailedException(InviteCodeGenerateFailedException e) {
        log.error("[GlobalExceptionHandler: handleInviteCodeGenerateFailedException 호출]", e);
        Sentry.captureException(e);
        return ErrorResponse.of(ErrorCode.COUPLE_CODE_GENERATE_FAILED);
    }

    @ExceptionHandler({TermsNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleTermsNotFoundException(TermsNotFoundException e) {
        log.warn("[GlobalExceptionHandler: handleTermsNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_TERMS);
    }

    @ExceptionHandler({LoveTypeNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleLoveTypeNotFoundException(LoveTypeNotFoundException e) {
        log.warn("[GlobalExceptionHandler: handleLoveTypeNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_LOVE_TYPE);
    }

    @ExceptionHandler({LoveTypeQuestionNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleLoveTypeQuestionNotFoundException(LoveTypeQuestionNotFoundException e) {
        log.warn("[GlobalExceptionHandler: handleLoveTypeQuestionNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_LOVE_TYPE_QUESTION);
    }

    @ExceptionHandler({NotCoupleMemberException.class})
    public ResponseEntity<ErrorResponse> handleNotCoupleMemberException(NotCoupleMemberException e) {
        log.info("[GlobalExceptionHandler: handleNotCoupleMemberException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NOT_COUPLE_MEMBER);
    }

    @ExceptionHandler({UsedInviteCodeException.class})
    public ResponseEntity<ErrorResponse> handleUsedCoupleCodeException(UsedInviteCodeException e) {
        log.info("[GlobalExceptionHandler: handleUsedCoupleCodeException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.USED_COUPLE_CODE);
    }

    @ExceptionHandler({AlreadyCoupledMemberException.class})
    public ResponseEntity<ErrorResponse> handleAlreadyCoupledMemberException(AlreadyCoupledMemberException e) {
        log.info("[GlobalExceptionHandler: handleAlreadyCoupledMemberException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.ALREADY_COUPLED_MEMBER);
    }

    @ExceptionHandler({SseConnectionException.class})
    public ResponseEntity<ErrorResponse> handleSseConnectionException(SseConnectionException e) {
        log.error("[GlobalExceptionHandler: handleSseConnectionException 호출]", e);
        Sentry.captureException(e);
        return ErrorResponse.of(ErrorCode.SSE_CONNECTION_ERROR);
    }

    @ExceptionHandler({MemberNotTestedException.class})
    public ResponseEntity<ErrorResponse> handleMemberNotTestedException(MemberNotTestedException e) {
        log.info("[GlobalExceptionHandler: handleMemberNotTestedException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.MEMBER_NOT_TESTED);
    }

    @ExceptionHandler({ChatRoomNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleChatRoomNotFoundException(ChatRoomNotFoundException e) {
        log.warn("[GlobalExceptionHandler: handleChatRoomNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_CHAT_ROOM);
    }

    @ExceptionHandler({NotValidChatRoomException.class})
    public ResponseEntity<ErrorResponse> handleNotValidChatRoomException(NotValidChatRoomException e) {
        log.warn("[GlobalExceptionHandler: handleNotValidChatRoomException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NOT_VALID_CHAT_ROOM);
    }

    @ExceptionHandler({MemberAccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleMemberAccessDeniedException(MemberAccessDeniedException e) {
        log.warn("[GlobalExceptionHandler: handleMemberAccessDeniedException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.MEMBER_ACCESS_DENIED);
    }

    @ExceptionHandler({NotValidCoupleCodeException.class})
    public ResponseEntity<ErrorResponse> handleNotValidCoupleCodeException(NotValidCoupleCodeException e) {
        log.info("[GlobalExceptionHandler: handleNotValidCoupleCodeException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NOT_VALID_COUPLE_CODE);
    }

    @ExceptionHandler({CoupleQuestionNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleCoupleQuestionNotFoundException(CoupleQuestionNotFoundException e) {
        log.warn("[GlobalExceptionHandler: handleCoupleQuestionNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_COUPLE_QUESTION);
    }

    @ExceptionHandler({OAuthUnlinkFailureException.class})
    public ResponseEntity<ErrorResponse> handleOAuthUnlinkFailureException(OAuthUnlinkFailureException e) {
        log.error("[GlobalExceptionHandler: handleOAuthUnlinkFailureException 호출]", e);
        Sentry.captureException(e);
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({TempLoveTypeNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleTempLoveTypeNotFoundException(TempLoveTypeNotFoundException e) {
        log.warn("[GlobalExceptionHandler: handleTempLoveTypeNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_TEMP_LOVE_TYPE);
    }

    @ExceptionHandler({BookmarkNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleBookmarkNotFoundException(BookmarkNotFoundException e) {
        log.warn("[GlobalExceptionHandler: handleBookmarkNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_BOOKMARK);
    }

    @ExceptionHandler({BookmarkAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handleBookmarkAlreadyExistsException(BookmarkAlreadyExistsException e) {
        log.info("[GlobalExceptionHandler: handleBookmarkAlreadyExistsException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.BOOKMARK_ALREADY_EXISTS);
    }

    @ExceptionHandler({MessageNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleMessageNotFoundException(MessageNotFoundException e) {
        log.warn("[GlobalExceptionHandler: handleMessageNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_MESSAGE);
    }

    @ExceptionHandler({PartnerProfileAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handlePartnerProfileAlreadyExistsException(PartnerProfileAlreadyExistsException e) {
        log.info("[GlobalExceptionHandler: handlePartnerProfileAlreadyExistsException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.PARTNER_PROFILE_ALREADY_EXISTS);
    }

    @ExceptionHandler({PartnerProfileNotFoundException.class})
    public ResponseEntity<ErrorResponse> handlePartnerProfileNotFoundException(PartnerProfileNotFoundException e) {
        log.info("[GlobalExceptionHandler: handlePartnerProfileNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_PARTNER_PROFILE);
    }

    @ExceptionHandler({LoveTypePersonalityTypeFeatureNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleLoveTypePersonalityTypeFeatureNotFoundException(LoveTypePersonalityTypeFeatureNotFoundException e) {
        log.warn("[GlobalExceptionHandler: handleLoveTypePersonalityTypeFeatureNotFoundException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.NO_SUCH_LOVE_TYPE_PERSONALITY_TYPE_RESULT);
    }



    /**
     *  ---------- 공통 예외 처리 핸들러 ----------
      */
    @ExceptionHandler({
            NoHandlerFoundException.class,
            TypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(Exception e) {
        log.warn("[CommonExceptionHandler: handleBadRequestException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("[CommonExceptionHandler: handleHttpRequestMethodNotSupportedException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler({IllegalArgumentException.class, IOException.class})
    public ResponseEntity<ErrorResponse> handleInternalServerException(Exception e) {
        log.error("[CommonExceptionHandler: handleInternalServerException 호출]", e);
        Sentry.captureException(e);
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(Exception e) {
        log.error("[CommonExceptionHandler: handleRuntimeException 호출]", e);
        Sentry.captureException(e);
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("[CommonExceptionHandler: handleMethodArgumentNotValidException 호출] {}", e.getMessage());
        return ErrorResponse.of(ErrorCode.BAD_REQUEST);
    }
}
