package makeus.cmc.malmo.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.out.LoadPromptPort;
import makeus.cmc.malmo.application.port.out.RequestChatApiPort;
import makeus.cmc.malmo.application.port.out.SaveChatMessageSummaryPort;
import makeus.cmc.malmo.application.port.out.SendSseEventPort;
import makeus.cmc.malmo.domain.model.chat.ChatMessage;
import makeus.cmc.malmo.domain.model.chat.ChatMessageSummary;
import makeus.cmc.malmo.domain.model.chat.Prompt;
import makeus.cmc.malmo.domain.service.ChatRoomDomainService;
import makeus.cmc.malmo.domain.service.MemberMemoryDomainService;
import makeus.cmc.malmo.domain.value.id.ChatRoomId;
import makeus.cmc.malmo.domain.value.id.MemberId;
import makeus.cmc.malmo.domain.service.ChatMessagesDomainService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChatStreamProcessor {

    private final RequestChatApiPort requestChatApiPort;
    private final ChatMessagesDomainService chatMessagesDomainService;
    private final ChatRoomDomainService chatRoomDomainService;
    private final MemberMemoryDomainService memberMemoryDomainService;
    private final SendSseEventPort sendSseEventPort;
    private final LoadPromptPort loadPromptPort;
    private final SaveChatMessageSummaryPort saveChatMessageSummaryPort;

    public void requestApiStream(MemberId memberId,
                                 boolean isMemberCoupled,
                                 Prompt systemPrompt,
                                 Prompt prompt,
                                 List<Map<String, String>> messages,
                                 ChatRoomId chatRoomId) {
        AtomicBoolean isOkDetected = new AtomicBoolean(false);
        messages.add(
                Map.of(
                        "role", "system",
                        "content", systemPrompt.getContent()
                )
        );
        messages.add(
                Map.of(
                        "role", "user",
                        "content", "[현재 단계 지시]\n" + prompt.getContent()
                )
        );

        // OpenAI API 스트리밍 호출
        requestChatApiPort.streamChat(messages,
                //  데이터 stream 수신 시 SSE 이벤트 전송
                chunk -> {
                    if(chunk.equals("OK")) {
                        isOkDetected.set(true);
                    } else {
                        sendSseMessage(memberId, chunk);
                    }
                },
                // 응답 완료 시 전체 응답 저장
                fullAnswer -> {
                    if (!isOkDetected.get()) {
                        saveAiMessage(memberId, chatRoomId, fullAnswer);
                    } else {
                        // chunk가 "OK"인 경우
                        //  ==========================< 메타데이터 수집 완료 단계 >=============================
                        //  - isLastPromptForMetaData = true
                        //      * 커플이 연동되지 않은 경우
                        //          - ChatRoom의 State를 PAUSED로 변경
                        //          - SSE 이벤트 chat_room_paused 전송
                        //      * 커플이 연동된 경우
                        //          - ChatRoom의 State 변경이나 SSE 이벤트 전송은 따로 하지 않고 고정된 멘트 전송
                        if (prompt.isLastPromptForMetadata()) {
                            if (!isMemberCoupled) {
                                // 커플이 연동되지 않은 경우
                                chatRoomDomainService.updateChatRoomStateToPaused(chatRoomId);
                                sendSseEventPort.sendToMember(
                                        memberId,
                                        new SendSseEventPort.NotificationEvent(
                                                SendSseEventPort.SseEventType.CHAT_ROOM_PAUSED,
                                                "메타데이터 수집이 완료되었습니다. 커플 연동을 해주세요."
                                        ));
                            } else {
                                // 커플이 연동된 경우
                                sendSseMessage(memberId, "오늘은 어떤 고민 때문에 나를 찾아왔어? 먼저 연인과 있었던 갈등 상황을 이야기해 주면 내가 같이 고민해볼게!");
                            }
                        } else {
                            //  ======================< 메타데이터 수집 or 일반적인 상담 >===========================
                            //  -> ChatRoom의 State를 NEED_NEXT_QUESTION로 변경
                            //  -> ChatRoom의 LEVEL을 다음 단계로 변경
                            //  -> SSE 이벤트 current_level_finished 전송 : 프론트에서 message 없이 재요청
                            //  -> 만약 예기치 못한 종료가 발생한 경우 : 프론트에서 ChatRoom의 State를 NEED_NEXT_QUESTION인 경우 재요청
                            //  ===============================================================================

                            // 다음 단계로 넘어가야 하는 상황
                            sendSseEventPort.sendToMember(
                                    memberId,
                                    new SendSseEventPort.NotificationEvent(
                                            SendSseEventPort.SseEventType.CURRENT_LEVEL_FINISHED,
                                            "현재 단계가 완료되었습니다. message 없이 재요청해주세요."
                                    ));
                        }
                    }
                },
                // 에러 발생 시 에러 메시지 전송
                errorMessage -> sendSseErrorMessage(memberId, errorMessage)
        );

    }

    // TODO : Message 요약 API 요청 Async Function
    //  - isCurrentPromptForMetaData = true
    //      => 메타데이터 수집 단계 (MemberMemory에 요약된 메타데이터 저장)
    //  - isCurrentPromptForMetaData = false
    //      => 일반적인 상담 단계 ChatMessageSummary (level=now,current=false)로 저장
    public void requestSummaryAsync(ChatRoomId chatRoomId, MemberId memberId, Prompt systemPrompt, Prompt prompt, Prompt summaryPrompt, boolean isForCurrentLevel, List<Map<String, String>> summaryRequestMessages) {
        summaryRequestMessages.add(
                Map.of(
                        "role", "system",
                        "content", systemPrompt.getContent()
                )
        );
        summaryRequestMessages.add(
                Map.of(
                        "role", "user",
                        "content", "[현재 단계 지시]\n" + summaryPrompt.getContent()
                )
        );

        log.info("요약 요청 시작: chatRoomId={}, memberId={}, isForCurrentLevel={}", chatRoomId, memberId, isForCurrentLevel);

        requestChatApiPort.requestSummary(
                summaryRequestMessages,
                summary -> {
                    log.info("요약 요청 완료: chatRoomId={}, memberId={}, summary={}", chatRoomId, memberId, summary);
                    if (prompt.isForMetadata()) {
                        // 메타데이터 수집 단계인 경우
                        memberMemoryDomainService.saveMemberMemory(memberId, summary);
                    } else {
                        // 일반적인 상담 단계인 경우
                        ChatMessageSummary chatMessageSummary = ChatMessageSummary.createChatMessageSummary(
                                chatRoomId, summary, prompt.getLevel(), isForCurrentLevel
                        );
                        saveChatMessageSummaryPort.saveChatMessageSummary(chatMessageSummary);
                    }

                    chatRoomDomainService.updateAllMessagesSummarized(chatRoomId);
                }
        );

    }

    private void saveAiMessage(MemberId memberId, ChatRoomId chatRoomId, String fullAnswer) {
        ChatMessage aiTextMessage = chatMessagesDomainService.createAiTextMessage(chatRoomId, fullAnswer);
        sendSseEventPort.sendToMember(
                memberId,
                new SendSseEventPort.NotificationEvent(
                        SendSseEventPort.SseEventType.AI_RESPONSE_ID,
                        aiTextMessage.getId()
                ));
    }

    private void sendSseMessage(MemberId memberId, String chunk) {
        // TODO : SSE Emitter 초기화 필요
        sendSseEventPort.sendToMember(
                memberId,
                new SendSseEventPort.NotificationEvent(
                        SendSseEventPort.SseEventType.CHAT_RESPONSE,
                        chunk
                ));
    }

    private void sendSseErrorMessage(MemberId memberId, String chunk) {
        sendSseEventPort.sendToMember(
                memberId,
                new SendSseEventPort.NotificationEvent(
                        SendSseEventPort.SseEventType.CHAT_RESPONSE,
                        chunk
                ));
    }
}
