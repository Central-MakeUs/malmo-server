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
    ALREADY_COUPLED_MEMBER(HttpStatus.BAD_REQUEST, 40007, "이미 커플로 등록된 사용자입니다. 커플 등록을 해제 후 이용해주세요."),
    NO_SUCH_CHAT_ROOM(HttpStatus.BAD_REQUEST, 40008, "채팅방이 존재하지 않습니다."),
    NOT_VALID_CHAT_ROOM(HttpStatus.BAD_REQUEST, 40009, "종료되었거나, 유효하지 않은 채팅방입니다."),
    MEMBER_NOT_TESTED(HttpStatus.BAD_REQUEST, 40010, "애착 유형 테스트를 완료하지 않은 사용자입니다. 애착 유형 테스트를 완료 후 이용해주세요."),
    NOT_VALID_COUPLE_CODE(HttpStatus.BAD_REQUEST, 40011, "유효하지 않은 커플 코드입니다."),
    NO_SUCH_COUPLE_QUESTION(HttpStatus.BAD_REQUEST, 40012, "커플 질문이 존재하지 않습니다."),
    NO_SUCH_TEMP_LOVE_TYPE(HttpStatus.BAD_REQUEST, 40013, "애착 유형 결과가 존재하지 않습니다."),
    NO_SUCH_BOOKMARK(HttpStatus.BAD_REQUEST, 40014, "북마크가 존재하지 않습니다."),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, 40015, "이미 북마크된 메시지입니다."),
    NO_SUCH_MESSAGE(HttpStatus.BAD_REQUEST, 40016, "메시지가 존재하지 않습니다."),
    PARTNER_PROFILE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, 40017, "이미 상대 프로필이 등록되어 있습니다."),
    NO_SUCH_PARTNER_PROFILE(HttpStatus.BAD_REQUEST, 40018, "등록된 상대 프로필이 존재하지 않습니다."),
    NO_SUCH_LOVE_TYPE_MBTI_RESULT(HttpStatus.BAD_REQUEST, 40019, "해당 MBTI와 애착 유형 결과가 존재하지 않습니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 40100, "인증되지 않은 사용자입니다."),
    INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, 40101, "ID Token 검증 과정에 오류가 발생하였습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, 40102, "Refresh Token 검증 과정에 오류가 발생하였습니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, 40300, "접근 권한이 없습니다."),
    NOT_COUPLE_MEMBER(HttpStatus.FORBIDDEN, 40301, "커플 등록 전인 사용자입니다. 커플 등록 후 이용해주세요."),
    MEMBER_ACCESS_DENIED(HttpStatus.FORBIDDEN, 40302, "사용자의 리소스 접근 권한이 없습니다."),

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
