package makeus.cmc.malmo.adaptor.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.web.docs.ApiCommonResponses;
import makeus.cmc.malmo.adaptor.in.web.docs.SwaggerResponses;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseListResponse;
import makeus.cmc.malmo.adaptor.in.web.dto.BaseResponse;
import makeus.cmc.malmo.application.port.in.GetCurrentChatRoomMessagesUseCase;
import makeus.cmc.malmo.application.port.in.GetCurrentChatRoomUseCase;
import makeus.cmc.malmo.application.port.in.SendChatMessageUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@Tag(name = "채팅 전송 API", description = "사용자 채팅 전송을 위한 API")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final GetCurrentChatRoomUseCase getCurrentChatRoomUseCase;
    private final GetCurrentChatRoomMessagesUseCase getCurrentChatRoomMessagesUseCase;


    // 채팅방 정보 조회 API 채팅방이 없으면 생성하고 전달
    @GetMapping("/current")
    public BaseResponse<GetCurrentChatRoomUseCase.GetCurrentChatRoomResponse> getCurrentChatRoom(
            @AuthenticationPrincipal User user) {
        GetCurrentChatRoomUseCase.GetCurrentChatRoomCommand command = GetCurrentChatRoomUseCase.GetCurrentChatRoomCommand.builder()
                .userId(Long.valueOf(user.getUsername()))
                .build();
        return BaseResponse.success(getCurrentChatRoomUseCase.getCurrentChatRoom(command));
    }

    @GetMapping("/current/messages")
    public BaseResponse<BaseListResponse<GetCurrentChatRoomMessagesUseCase.ChatRoomMessageDto>> getCurrentChatRoomMessages(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        GetCurrentChatRoomMessagesUseCase.GetCurrentChatRoomMessagesCommand command = GetCurrentChatRoomMessagesUseCase.GetCurrentChatRoomMessagesCommand.builder()
                .userId(Long.valueOf(user.getUsername()))
                .page(page) // 기본 페이지 0
                .size(size) // 기본 페이지 크기 20
                .build();
        GetCurrentChatRoomMessagesUseCase.GetCurrentChatRoomMessagesResponse currentChatRoomMessages = getCurrentChatRoomMessagesUseCase.getCurrentChatRoomMessages(command);

        return BaseListResponse.success(currentChatRoomMessages.getMessages());
    }

    @Operation(
            summary = "채팅 메시지 전송",
            description = "서버로 AI 상담을 위한 사용자의 메시지를 전달합니다. AI 응답은 SSE로 전달됩니다. JWT 토큰이 필요합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponse(
            responseCode = "200",
            description = "메시지 전송 성공",
            content = @Content(schema = @Schema(implementation = SwaggerResponses.SendChatSuccessResponse.class))
    )
    @ApiCommonResponses.RequireAuth
    @PostMapping("/send")
    public BaseResponse<SendChatMessageUseCase.SendChatMessageResponse> sendChatMessage(
            @AuthenticationPrincipal User user,
            @Valid ChatRequest request) {
        SendChatMessageUseCase.SendChatMessageResponse sendChatMessageResponse = sendChatMessageUseCase.processUserMessage(
                SendChatMessageUseCase.SendChatMessageCommand.builder()
                        .userId(Long.valueOf(user.getUsername()))
                        .message(request.getMessage())
                        .build());

        return BaseResponse.success(sendChatMessageResponse);
    }

    @PostMapping("/room/upgrade")
    public BaseResponse<SendChatMessageUseCase.SendChatMessageResponse> sendChatMessage(
            @AuthenticationPrincipal User user) {
        SendChatMessageUseCase.SendChatMessageResponse sendChatMessageResponse = sendChatMessageUseCase.upgradeChatRoom(
                SendChatMessageUseCase.SendChatMessageCommand.builder()
                        .userId(Long.valueOf(user.getUsername()))
                        .build());

        return BaseResponse.success(sendChatMessageResponse);
    }

    @Getter
    @AllArgsConstructor
    public static class ChatRequest {
        @NotBlank(message = "메시지는 비어있을 수 없습니다.")
        private String message;
    }
}
