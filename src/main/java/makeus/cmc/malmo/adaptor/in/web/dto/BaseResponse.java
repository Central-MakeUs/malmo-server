package makeus.cmc.malmo.adaptor.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.MDC;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
    private String requestId;
    private boolean success;
    private String message;
    private T data;

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(MDC.get("request_id"), true, "요청이 성공적으로 처리되었습니다.", data);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(MDC.get("request_id"), true, message, data);
    }

    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(MDC.get("request_id"), true, "요청이 성공적으로 처리되었습니다.", null);
    }
}
