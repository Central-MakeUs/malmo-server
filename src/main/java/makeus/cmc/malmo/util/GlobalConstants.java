package makeus.cmc.malmo.util;

public class GlobalConstants {
    // 오늘의 질문 상수
    public static final int FIRST_QUESTION_LEVEL = 1;

    // 채팅방 관련 상수
    public static final int INIT_CHATROOM_LEVEL = 1;
    public static final String INIT_CHAT_MESSAGE_FIRST = " 안녕!";
    public static final String INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE =
            "오늘은 어떤 고민 때문에 나를 찾아왔어? 마음에 두고 있는 상대와 있었던 상황을 이야기해 주면 내가 같이 고민해볼게!";

    public static final String INIT_CHAT_MESSAGE_SECOND_IN_RELATIONSHIP =
            "오늘은 어떤 고민 때문에 나를 찾아왔어? 먼저 연인과 있었던 갈등 상황을 이야기해 주면 내가 같이 고민해볼게!";

    public static final String INIT_CHAT_MESSAGE_SECOND_BREAKUP =
            "오늘은 어떤 고민 때문에 나를 찾아왔어? 이별 전후로 마음에 남아 있는 상황을 이야기해 주면 내가 같이 고민해볼게!";

    public static final String ATTACHMENT_TYPE_PROMPT_MESSAGE =
            "잠깐! 애착유형 테스트를 하면, 더 정확한 상담이 가능해! 그대로 진행하면 바로 상담해줄게";

    public static final String PARTNER_LOVE_TYPE_INFERENCE_PROMPT = """
            너는 연애 상담 대화의 2단계 분석 내용을 바탕으로 상대방의 애착유형을 추론하는 분류기야.
            지금 주어지는 컨텍스트는 2단계 상담 맥락, 2단계 대화 내용, 그리고 챗봇이 수행한 2단계 분석을 포함한다.
            반드시 이 2단계 분석과 동일한 맥락만 사용해서 상대방의 애착유형을 추론해라.

            [분류 규칙]
            - partnerLoveTypeCategory는 반드시 다음 4개 중 하나만 선택한다.
              STABLE_TYPE
              ANXIETY_TYPE
              AVOIDANCE_TYPE
              CONFUSION_TYPE
            - UNKNOWN은 절대 반환하지 않는다.
            - 설명, 이유, 추가 텍스트 없이 JSON 객체만 반환한다.

            [응답 형식]
            {"partnerLoveTypeCategory":"STABLE_TYPE"}
            """;
    // 커플 복구 관련 상수
    public static final int COUPLE_RECOVERY_LIMIT_DAYS = 30;

}
