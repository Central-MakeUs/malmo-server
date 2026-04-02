package makeus.cmc.malmo.adaptor.message;

public enum StreamMessageType {
    REQUEST_CHAT_MESSAGE,
    REQUEST_EXTRACT_METADATA,
    REQUEST_TITLE_GENERATION,  // 제목 생성 요청
    REQUEST_CONVERSATION_SUMMARY,  // 4단계 대화 요약 요청
    REQUEST_WEEKLY_ANALYSIS_REPORT
}
