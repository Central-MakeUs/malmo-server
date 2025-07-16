package makeus.cmc.malmo.adaptor.in.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, 40000, "잘못된 요청입니다."),
    NO_SUCH_MEMBER(HttpStatus.BAD_REQUEST, 40001, "멤버가 존재하지 않습니다."),
    NO_SUCH_COUPLE_CODE(HttpStatus.BAD_REQUEST, 40002, "초대 코드가 존재하지 않습니다."),
    NO_SUCH_TERMS(HttpStatus.BAD_REQUEST, 40003, "약관이 존재하지 않습니다."),
    NO_SUCH_LOVE_TYPE(HttpStatus.BAD_REQUEST, 40004, "애착 유형이 존재하지 않습니다."),
    NO_SUCH_LOVE_TYPE_QUESTION(HttpStatus.BAD_REQUEST, 40005, "애착 유형 질문이 존재하지 않습니다."),
    USED_COUPLE_CODE(HttpStatus.BAD_REQUEST, 40006, "이미 사용된 초대 코드입니다."),
    TERMS_ALREADY_AGREED(HttpStatus.BAD_REQUEST, 40007, "이미 동의한 약관입니다."),
    TERMS_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, 40008, "이미 존재하는 약관입니다."),
    ALREADY_COUPLED_MEMBER(HttpStatus.BAD_REQUEST, 40009, "이미 커플로 등록된 사용자입니다. 커플 등록을 해제 후 이용해주세요."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 40100, "인증되지 않은 사용자입니다."),
    INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, 40101, "ID Token 검증 과정에 오류가 발생하였습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, 40102, "Refresh Token 검증 과정에 오류가 발생하였습니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, 40300, "접근 권한이 없습니다."),
    NOT_COUPLE_MEMBER(HttpStatus.FORBIDDEN, 40301, "커플 등록 전인 사용자입니다. 커플 등록 후 이용해주세요."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, 40400, "요청한 리소스를 찾을 수 없습니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 40500, "허용되지 않은 HTTP 메소드입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "서버 내부 오류입니다."),
    COUPLE_CODE_GENERATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 50001, "커플 코드 생성에 실패했습니다."),
    SSE_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50002, "SSE 연결 중 오류가 발생했습니다."),

    // 502 Bad Gateway
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, 50200, "외부 API 호출 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
