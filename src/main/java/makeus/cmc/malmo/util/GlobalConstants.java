package makeus.cmc.malmo.util;

public class GlobalConstants {
    // 오늘의 질문 상수
    public static final int FIRST_QUESTION_LEVEL = 1;

    // 채팅방 관련 상수
    public static final int INIT_CHATROOM_LEVEL = 1;
    public static final String INIT_CHAT_MESSAGE_FIRST = " 안녕!";
    public static final String INIT_CHAT_MESSAGE_SECOND_PREFIX = "나는 연애 고민 상담사 모모야.\n" +
            "나와의 대화를 마무리하고 싶다면 종료하기 버튼을 눌러줘! 대화 종료 후에는 대화 요약 리포트를 보여줄게.\n";

    public static final String INIT_CHAT_MESSAGE_SECOND_SEEING_SOMEONE =
            INIT_CHAT_MESSAGE_SECOND_PREFIX +
            "오늘은 어떤 고민 때문에 나를 찾아왔어? 마음에 두고 있는 상대와 있었던 상황을 이야기해 주면 내가 같이 고민해볼게!";

    public static final String INIT_CHAT_MESSAGE_SECOND_IN_RELATIONSHIP =
            INIT_CHAT_MESSAGE_SECOND_PREFIX +
            "오늘은 어떤 고민 때문에 나를 찾아왔어? 먼저 연인과 있었던 갈등 상황을 이야기해 주면 내가 같이 고민해볼게!";

    public static final String INIT_CHAT_MESSAGE_SECOND_BREAKUP =
            INIT_CHAT_MESSAGE_SECOND_PREFIX +
            "오늘은 어떤 고민 때문에 나를 찾아왔어? 이별 전후로 마음에 남아 있는 상황을 이야기해 주면 내가 같이 고민해볼게!";

    public static final String OPENAI_CHAT_URL = "https://api.openai.com/v1";

    public static final String OPENAI_STATUS_URL = "https://status.openai.com/api/v2/status.json";

    public static final String ATTACHMENT_TYPE_PROMPT_MESSAGE =
            "잠깐! 애착유형 테스트를 하면, 더 정확한 상담이 가능해! 그대로 진행하면 바로 상담해줄게";
    // 커플 복구 관련 상수
    public static final int COUPLE_RECOVERY_LIMIT_DAYS = 30;

}
