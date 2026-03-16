package makeus.cmc.malmo.adaptor.in.web.docs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import makeus.cmc.malmo.application.port.in.notification.GetPendingNotificationUseCase;
import makeus.cmc.malmo.domain.value.state.ChatRoomState;
import makeus.cmc.malmo.domain.value.state.MemberState;
import makeus.cmc.malmo.domain.value.state.NotificationState;
import makeus.cmc.malmo.domain.value.state.TermsDetailsType;
import makeus.cmc.malmo.domain.value.type.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SwaggerResponses {

    @Getter
    @Schema(description = "기본 응답 형식")
    public static class BaseSwaggerResponse<T> {
        @Schema(description = "요청 ID", example = "e762d840-9565-4612-b308-42d1a50dc0c2")
        private String requestId;

        @Schema(description = "성공 여부", example = "true")
        private boolean success;

        @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
        private String message;

        @Schema(description = "응답 데이터")
        private T data;
    }

    @Getter
    @Schema(description = "기본 리스트 응답 형식")
    public static class BaseListSwaggerResponse<T> {
        @Schema(description = "응답 데이터 크기", example = "10")
        private int size;

        @Schema(description = "페이지 번호", example = "0")
        private Integer page;

        @Schema(description = "응답 데이터 리스트")
        private List<T> list;

        @Schema(description = "전체 데이터 개수", example = "100")
        private Long totalCount;
    }

    // 로그인 관련 응답
    @Getter
    @Schema(description = "로그인 성공 응답")
    public static class LoginSuccessResponse extends BaseSwaggerResponse<LoginData> {
    }

    @Getter
    @Schema(description = "로그아웃 성공 응답")
    public static class LogoutSuccessResponse extends BaseSwaggerResponse<Void> {
    }

    @Getter
    @Schema(description = "토큰 갱신 성공 응답")
    public static class RefreshTokenSuccessResponse extends BaseSwaggerResponse<TokenData> {
    }

    // 회원가입 관련 응답
    @Getter
    @Schema(description = "회원가입 성공 응답")
    public static class SignUpSuccessResponse extends BaseSwaggerResponse {
    }

    // 멤버 관련 응답
    @Getter
    @Schema(description = "멤버 정보 조회 성공 응답")
    public static class MemberInfoSuccessResponse extends BaseSwaggerResponse<MemberData> {
    }

    @Getter
    @Schema(description = "파트너 멤버 정보 조회 성공 응답")
    public static class PartnerMemberInfoSuccessResponse extends BaseSwaggerResponse<PartnerMemberData> {
    }

    @Getter
    @Schema(description = "상대 프로필 등록/수정 성공 응답")
    public static class PartnerProfileSuccessResponse extends BaseSwaggerResponse<PartnerProfileData> {
    }

    @Getter
    @Schema(description = "멤버 정보 수정 성공 응답")
    public static class UpdateMemberSuccessResponse extends BaseSwaggerResponse<UpdateMemberData> {
    }

    @Getter
    @Schema(description = "멤버 탈퇴 성공 응답")
    public static class DeleteMemberSuccessResponse extends BaseSwaggerResponse<DeleteMemberData> {
    }

    @Getter
    @Schema(description = "멤버 약관 동의 수정 성공 응답")
    public static class UpdateMemberTermsSuccessResponse extends BaseSwaggerResponse<BaseListSwaggerResponse<TermsData>> {
    }

    @Getter
    @Schema(description = "멤버 미조회 알림 조회 성공 응답")
    public static class GetPendingMemberNotificationSuccessResponse extends BaseSwaggerResponse<BaseListSwaggerResponse<PendingNotificationData>> {
    }

    @Getter
    @Schema(description = "멤버 미조회 알림 조회 처리 성공 응답")
    public static class ReadMemberNotificationSuccessResponse extends BaseSwaggerResponse<Void> {
    }

    @Getter
    @Deprecated
    @Schema(description = "[Deprecated] 멤버 초대 코드 성공 응답")
    public static class GetInviteCodeSuccessResponse extends BaseSwaggerResponse<InviteCodeResponseData> {
    }

    // 커플 관련 응답
    @Getter
    @Deprecated
    @Schema(description = "[Deprecated] 커플 연결 성공 응답")
    public static class CoupleLinkSuccessResponse extends BaseSwaggerResponse<CoupleLinkData> {
    }

    @Getter
    @Deprecated
    @Schema(description = "[Deprecated] 커플 연결 끊기 성공 응답")
    public static class CoupleUnlinkSuccessResponse extends BaseSwaggerResponse<CoupleUnlinkData> {
    }

    // 애착유형 관련 응답
    @Getter
    @Schema(description = "애착유형 질문 조회 성공 응답")
    public static class LoveTypeQuestionSuccessResponse extends BaseSwaggerResponse<BaseListSwaggerResponse<LoveTypeQuestionData>> {
    }

    @Getter
    @Schema(description = "애착유형 질문 결과 등록 성공 응답")
    public static class LoveTypeQuestionCalculateSuccessResponse extends BaseSwaggerResponse<LoveTypeQuestionCalculationData> {
    }

    @Getter
    @Schema(description = "MBTI + 애착유형 상세 결과 조회 성공 응답")
    public static class LoveTypeMbtiResultSuccessResponse extends BaseSwaggerResponse<LoveTypeMbtiResultData> {
    }

    @Getter
    @Schema(description = "애착유형 등록 성공 응답")
    public static class RegisterLoveTypeSuccessResponse extends BaseSwaggerResponse<Void> {
    }

    @Getter
    @Deprecated
    @Schema(description = "[Deprecated] 연애 시작일 갱신 성공 응답")
    public static class UpdateStartLoveDateSuccessResponse extends BaseSwaggerResponse<UpdateStartLoveDateData> {
    }

    // 질문 관련 응답
    @Getter
    @Schema(description = "오늘의 질문 조회 성공 응답")
    public static class QuestionSuccessResponse extends BaseSwaggerResponse<QuestionData> {
    }

    @Getter
    @Schema(description = "질문 답변 등록 성공 응답")
    public static class AnswerSuccessResponse extends BaseSwaggerResponse<AnswerData> {
    }

    @Getter
    @Schema(description = "과거 질문 조회 성공 응답")
    public static class PastQuestionSuccessResponse extends BaseSwaggerResponse<PastQuestionData> {
    }

    @Getter
    @Schema(description = "질문 답변 조회 성공 응답")
    public static class PastAnswerSuccessResponse extends BaseSwaggerResponse<PastAnswerData> {
    }

    // 약관 관련 응답
    @Getter
    @Schema(description = "약관 목록 조회 성공 응답")
    public static class TermsListSuccessResponse extends BaseSwaggerResponse<BaseListSwaggerResponse<TermsResponseData>> {
    }

    // 채팅 관련 응답
    @Getter
    @Schema(description = "채팅 전송 성공 응답")
    public static class SendChatSuccessResponse extends BaseSwaggerResponse<SendChatData> {
    }

    @Getter
    @Schema(description = "채팅방 상태 조회 성공 응답")
    public static class ChatRoomStateResponse extends BaseSwaggerResponse<ChatRoomStateData> {
    }

    @Getter
    @Schema(description = "채팅 메시지 리스트 조회 성공 응답")
    public static class ChatMessageListSuccessResponse extends BaseSwaggerResponse<BaseListSwaggerResponse<ChatRoomMessageData>> {
    }

    @Getter
    @Schema(description = "채팅방 완료 성공 응답")
    public static class CompleteChatRoomResponse extends BaseSwaggerResponse<CompleteChatRoomData> {
    }

    @Getter
    @Schema(description = "채팅방 요약 조회 성공 응답")
    public static class GetChatRoomSummaryResponse extends BaseSwaggerResponse<GetChatRoomSummaryData> {
    }

    @Getter
    @Schema(description = "채팅방 리스트 조회 성공 응답")
    public static class ChatRoomListSuccessResponse extends BaseSwaggerResponse<BaseListSwaggerResponse<GetChatRoomListResponse>> {
    }

    @Getter
    @Schema(description = "채팅방 삭제 성공 응답")
    public static class ChatRoomDeleteSuccessResponse extends BaseSwaggerResponse {
    }

    // 데이터 클래스들
    @Getter
    @Schema(description = "로그인 응답 데이터")
    public static class LoginData {
        @Schema(description = "토큰 타입", example = "Bearer")
        private String grantType;

        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String accessToken;

        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String refreshToken;

        @Schema(description = "멤버 상태", example = "ALIVE")
        private String memberState;
    }

    @Getter
    @Schema(description = "토큰 응답 데이터")
    public static class TokenData {
        @Schema(description = "토큰 타입", example = "Bearer")
        private String grantType;

        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String accessToken;

        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String refreshToken;
    }

    @Getter
    @Schema(description = "멤버 정보 응답 데이터")
    public static class MemberData {
        @Schema(description = "멤버 상태", example = "ALIVE")
        private MemberState memberState;

        @Schema(description = "로그인한 Provider", example = "KAKAO")
        private Provider provider;

        @Schema(description = "연애 시작일", example = "2023-01-15")
        private LocalDate startLoveDate;

        @Schema(description = "애착 유형", example = "STABLE_TYPE")
        private LoveTypeCategory loveTypeCategory;

        @Schema(description = "완료된 채팅방 개수", example = "5")
        private int totalChatRoomCount;

        @Schema(description = "총 할당된 오늘의 질문 개수", example = "10")
        private int totalCoupleQuestionCount;

        @Schema(description = "회피 비율", example = "0.3")
        private float avoidanceRate;

        @Schema(description = "불안 비율", example = "0.2")
        private float anxietyRate;

        @Schema(description = "닉네임", example = "홍길동")
        private String nickname;

        @Schema(description = "이메일", example = "test@example.com")
        private String email;

        @Schema(description = "연애 상태", example = "IN_RELATIONSHIP")
        private RelationshipStatus relationshipStatus;

        @Schema(description = "내 MBTI", example = "INTJ")
        private String mbti;

        @Schema(description = "상대방 MBTI", example = "ENFP")
        private String partnerMbti;

        @Schema(description = "상대방 애착 유형", example = "UNKNOWN")
        private PartnerLoveTypeCategory partnerLoveTypeCategory;
    }

    @Getter
    @Deprecated
    @Schema(description = "[Deprecated] 상대 프로필 조회 응답 데이터")
    public static class PartnerMemberData {
        @Schema(description = "상대방 MBTI", example = "ENFP")
        private String mbti;

        @Schema(description = "상대방 애착 유형", example = "UNKNOWN")
        private PartnerLoveTypeCategory loveTypeCategory;

        @Schema(description = "애착 유형 설명", example = "모르겠어요")
        private String description;
    }

    @Getter
    @Schema(description = "상대 프로필 응답 데이터")
    public static class PartnerProfileData {
        @Schema(description = "상대방 MBTI", example = "ENFP")
        private String mbti;

        @Schema(description = "상대방 애착 유형", example = "UNKNOWN")
        private PartnerLoveTypeCategory loveTypeCategory;

        @Schema(description = "애착 유형 설명", example = "모르겠어요")
        private String description;
    }

    @Getter
    @Schema(description = "멤버 정보 수정 응답 데이터")
    public static class UpdateMemberData {
        @Schema(description = "닉네임", example = "홍길동")
        private String nickname;

        @Schema(description = "연애 상태", example = "IN_RELATIONSHIP")
        private RelationshipStatus relationshipStatus;

        @Schema(description = "내 MBTI", example = "INTJ")
        private String mbti;

        @Schema(description = "내 애착 유형", example = "STABLE_TYPE")
        private LoveTypeCategory loveTypeCategory;
    }

    @Data
    @Builder
    public static class PendingNotificationData {
        @Schema(description = "알림 ID", example = "1")
        private Long id;

        @Schema(description = "알림 타입", example = "COUPLE_DISCONNECTED")
        private NotificationType type;

        @Schema(description = "알림 상태", example = "PENDING")
        private NotificationState state;

        @Schema(description = "알림 세부사항", example = "알람 마다 다른 내용이 들어갑니다.")
        private Map<String, Object> payload;

        @Schema(description = "알림 생성시간", example = "2023-07-03T10:00:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Schema(description = "멤버 탈퇴 응답 데이터")
    public static class DeleteMemberData {
    }

    @Getter
    @Schema(description = "약관 동의 정보 데이터")
    public static class TermsData {
        @Schema(description = "약관 ID", example = "1")
        private Long termsId;

        @Schema(description = "약관 동의 여부", example = "true")
        private Boolean isAgreed;
    }

    @Getter
    @Deprecated
    @Schema(description = "[Deprecated] 커플 연결 응답 데이터")
    public static class CoupleLinkData {
        @Schema(description = "생성된 커플 ID", example = "1")
        private Long coupleId;
    }

    @Getter
    @Deprecated
    @Schema(description = "[Deprecated] 커플 연결 끊기 응답 데이터")
    public static class CoupleUnlinkData {
        @Schema(description = "해제된 커플 ID", example = "1")
        private Long coupleId;
    }

    @Getter
    @Schema(description = "애착유형 질문 조회 응답 데이터")
    public static class LoveTypeQuestionData {
        @Schema(description = "질문 번호", example = "1")
        private int questionNumber;
        @Schema(description = "내용", example = "나는 연인에게 모든 것을 다 이야기한다")
        private String content;
    }

    @Getter
    @Schema(description = "애착유형 질문 응답 등록 결과 데이터")
    public static class LoveTypeQuestionCalculationData {
        @Schema(description = "임시 애착유형 결과 ID", example = "1")
        private Long loveTypeId;

        @Schema(description = "애착 유형", example = "STABLE_TYPE")
        private LoveTypeCategory loveTypeCategory;

        @Schema(description = "회피 비율", example = "0.3")
        private float avoidanceRate;

        @Schema(description = "불안 비율", example = "0.2")
        private float anxietyRate;
    }

    @Getter
    @Schema(description = "MBTI + 애착유형 상세 결과 응답 데이터")
    public static class LoveTypeMbtiResultData {
        @Schema(description = "MBTI", example = "ENFP")
        private String mbti;

        @Schema(description = "애착 유형", example = "STABLE_TYPE")
        private LoveTypeCategory loveTypeCategory;

        @Schema(description = "요약", example = "풍부한 상상력과 사랑으로, 함께하는 일상을 즐겁게 만들어 가는 유형")
        private String summary;

        @Schema(description = "키워드 목록")
        private List<String> keywords;

        @Schema(description = "강점 목록")
        private List<LoveTypeTextBlockData> strengths;

        @Schema(description = "약점 목록")
        private List<LoveTypeTextBlockData> weaknesses;

        @Schema(description = "관계 패턴 목록")
        private List<LoveTypeTextBlockData> patterns;

        @Schema(description = "애착유형 특징 목록")
        private List<LoveTypeTextBlockData> loveTypeFeatures;

        @Schema(description = "연애 가이드 목록")
        private List<String> datingGuides;

        @Schema(description = "잘 맞는 MBTI 목록")
        private List<LoveTypeMbtiBlockData> bestMatches;

        @Schema(description = "부딪히기 쉬운 MBTI 목록")
        private List<LoveTypeMbtiBlockData> worstMatches;
    }

    @Getter
    @Schema(description = "제목 + 설명 텍스트 블록")
    public static class LoveTypeTextBlockData {
        @Schema(description = "제목", example = "Ne")
        private String title;

        @Schema(description = "설명", example = "흩어진 정보 속에서 하나의 핵심 맥락과 미래를 읽어내요")
        private String description;
    }

    @Getter
    @Schema(description = "MBTI + 설명 블록")
    public static class LoveTypeMbtiBlockData {
        @Schema(description = "MBTI", example = "INFJ")
        private String mbti;

        @Schema(description = "설명", example = "속마음을 깊이 이해해주며 안정적인 감정을 공유하는 궁합")
        private String description;
    }

    @Getter
    @Deprecated
    @Schema(description = "[Deprecated] 연애 시작일 갱신 응답 데이터")
    public static class UpdateStartLoveDateData {
        @Schema(description = "변경된 연애 시작일", example = "2023-01-15")
        private LocalDate startLoveDate;
    }

    @Getter
    @Schema(description = "질문 응답 데이터")
    public static class QuestionData {
        @Schema(description = "커플 질문 ID", example = "1")
        private Long coupleQuestionId;

        @Schema(description = "질문 단계", example = "3")
        private int level;

        @Schema(description = "질문 제목", example = "오늘 하루 어땠나요?")
        private String title;

        @Schema(description = "질문 내용", example = "오늘 하루 중 가장 기억에 남는 순간은 무엇인가요?")
        private String content;

        @Schema(description = "나의 답변 여부", example = "true")
        private boolean meAnswered;

        @Schema(description = "상대방 답변 여부", example = "false")
        private boolean partnerAnswered;

        @Schema(description = "생성일시", example = "2023-07-03T10:00:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Schema(description = "질문 리스트 응답 데이터")
    public static class QuestionListData {
        @Schema(description = "커플 질문 ID", example = "1")
        private Long coupleQuestionId;

        @Schema(description = "질문 제목", example = "오늘 하루 어땠나요?")
        private String title;

        @Schema(description = "질문 내용", example = "오늘 하루 중 가장 기억에 남는 순간은 무엇인가요?")
        private String content;

        @Schema(description = "생성일시", example = "2023-07-03T10:00:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Schema(description = "답변 등록 응답 데이터")
    public static class AnswerData {
        @Schema(description = "답변이 달린 질문의 ID", example = "1")
        private Long coupleQuestionId;
    }

    @Getter
    @Schema(description = "과거 질문 조회 응답 데이터")
    public static class PastQuestionData {
        @Schema(description = "커플 질문 ID", example = "1")
        private Long coupleQuestionId;

        @Schema(description = "질문 단계", example = "3")
        private int level;

        @Schema(description = "질문 제목", example = "오늘 하루 어땠나요?")
        private String title;

        @Schema(description = "질문 내용", example = "오늘 하루 중 가장 기억에 남는 순간은 무엇인가요?")
        private String content;

        @Schema(description = "나의 답변 여부", example = "true")
        private boolean meAnswered;

        @Schema(description = "상대방 답변 여부", example = "false")
        private boolean partnerAnswered;

        @Schema(description = "생성일시", example = "2023-07-03T10:00:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Schema(description = "과거 답변 조회 응답 데이터")
    public static class PastAnswerData {
        @Schema(description = "질문 제목", example = "오늘 하루 어땠나요?")
        private String title;
        @Schema(description = "질문 내용", example = "오늘 하루 중 가장 기억에 남는 순간은 무엇인가요?")
        private String content;
        @Schema(description = "질문 단계", example = "3")
        private int level;
        @Schema(description = "답변 생성일시", example = "2023-07-03T10:00:00")
        private LocalDateTime createdAt;

        @Schema(description = "멤버 답변 정보")
        private AnswerDto me;
        @Schema(description = "상대방 답변 정보")
        private AnswerDto partner;
    }

    @Getter
    @Schema(description = "멤버 답변 상세 데이터")
    class AnswerDto {
        @Schema(description = "답변한 멤버 닉네임", example = "홍길동")
        private String nickname;
        @Schema(description = "답변 내용", example = "오늘은 정말 좋은 하루였어요!")
        private String answer;
        @Schema(description = "수정 가능", example = "true")
        private boolean updatable;
    }

    @Getter
    @Schema(description = "약관 응답 데이터")
    public static class TermsResponseData {
        @Schema(description = "약관 타입", example = "AGE_VERIFICATION")
        private TermsType termsType;
        @Schema(description = "약관 내용 데이터")
        private TermsContentResponseData content;
    }

    @Getter
    @Schema(description = "약관 내용 응답 데이터")
    public static class TermsContentResponseData {
        @Schema(description = "약관 ID", example = "1")
        private Long termsId;

        @Schema(description = "약관 제목", example = "서비스 이용약관")
        private String title;

        @Schema(description = "약관 내용 및 형식")
        private List<TermsDetailsResponseData> details;

        @Schema(description = "필수 동의 여부", example = "true")
        private Boolean isRequired;

        @Schema(description = "생성일시", example = "2023-07-03T10:00:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Schema(description = "약관 내용 및 형식 데이터")
    public static class TermsDetailsResponseData {
        @Schema(description = "해당 내용의 형식", example = "TITLE")
        private TermsDetailsType type;

        @Schema(description = "약관 내용", example = "서비스 이용약관에 동의합니다.")
        private String content;
    }

    @Getter
    @Deprecated
    @Schema(description = "[Deprecated] 초대 코드 응답 데이터")
    public static class InviteCodeResponseData {
        private String coupleCode;
    }

    @Getter
    @Schema(description = "채팅 응답 데이터")
    public static class SendChatData {
        @Schema(description = "사용자가 보낸 메시지의 ID", example = "1")
        private Long messageId;
    }

    @Getter
    @Schema(description = "채팅방 생성 성공 응답")
    public static class CreateChatRoomResponse extends BaseSwaggerResponse<CreateChatRoomData> {
    }

    @Getter
    @Schema(description = "채팅방 생성 응답 데이터")
    public static class CreateChatRoomData {
        @Schema(description = "채팅방 ID", example = "123")
        private Long chatRoomId;
        @Schema(description = "채팅방 상태", example = "ALIVE")
        private ChatRoomState chatRoomState;
        @Schema(description = "생성 시간", example = "2026-01-12T15:30:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Schema(description = "채팅방 상태 응답 데이터")
    public static class ChatRoomStateData {
        @Schema(description = "현재 채팅방의 ID", example = "1")
        private Long chatRoomId;

        @Schema(description = "현재 채팅방의 상태", example = "ALIVE")
        private ChatRoomState chatRoomState;
    }

    @Getter
    @Schema(description = "채팅방 메시지 응답 데이터")
    class ChatRoomMessageData {
        @Schema(description = "채팅 메시지 ID", example = "1")
        private Long messageId;
        @Schema(description = "채팅 전송자(유저, 모모)", example = "USER")
        private SenderType senderType;
        @Schema(description = "채팅 내용", example = "안녕?")
        private String content;
        @Schema(description = "채팅 생성 시간", example = "2025-07-20T10:15:30")
        private LocalDateTime createdAt;
        @Schema(description = "해당 메시지에 대한 북마크 ID (없으면 null)", example = "10")
        private Long bookmarkId;
    }

    @Getter
    @Schema(description = "채팅 완료 응답 데이터")
    public static class CompleteChatRoomData {
        @Schema(description = "채팅방의 ID", example = "1")
        private Long chatRoomId;
    }

    @Getter
    @Schema(description = "채팅 요약 조회 완료 응답 데이터")
    public static class GetChatRoomSummaryData {
        @Schema(description = "채팅방의 ID", example = "1")
        private Long chatRoomId;
        @Schema(description = "채팅방 생성 시간", example = "2025-07-20T10:15:30")
        private LocalDateTime createdAt;
        @Schema(description = "채팅방 전체 요약", example = "회피형 남자친구의 연락두절 문제")
        private String totalSummary;
        @Schema(description = "채팅방 상황 요약", example = "남자친구는 여사친과 몰래 밥을 먹은 일에 대해 사과하길 회피했다. 이전에도 비슷한 상황이 반복되었고, 베리는 자신의 감정을 과한 것으로 여기며 소통에 어려움을 경험했다.")
        private String firstSummary;
        @Schema(description = "채팅방 관계 이해", example = "회피형 성향의 남자친구는 비난으로 느껴지는 말과 요구에 부담을 느끼고, 불안형 성향의 베리는 명확한 애정 표현과 확인을 요구하면서 두 사람의 갈등이 심화되고 있다.")
        private String secondSummary;
        @Schema(description = "채팅방 해결 제안", example = "남자친구가 방어적이지 않도록, 대화 전 일정한 거리를 두고 대화하길 추천함. 대화 목적이 관계 개선임을 먼저 짚고, 자기방어형 말하기에는 상대의 의도를 인정하면서도 감정을 정리해 전하는 것이 중요함. 예: “네가 그런 의도가 아니었다는 건 알지만, 나는 그 말에 힘들었어.")
        private String thirdSummary;
    }

    @Getter
    @Schema(description = "채팅 리스트 조회 완료 응답 데이터")
    public static class GetChatRoomListResponse {
        @Schema(description = "채팅방의 ID", example = "1")
        private Long chatRoomId;
        @Schema(description = "채팅방 전체 요약", example = "회피형 남자친구의 연락두절 문제")
        private String totalSummary;
        @Schema(description = "채팅방 상황 키워드", example = "연락 회피")
        private String situationKeyword;
        @Schema(description = "채팅방 해결 키워드", example = "완충 표현")
        private String solutionKeyword;
        @Schema(description = "채팅방 생성 시간", example = "2025-07-20T10:15:30")
        private LocalDateTime createdAt;
    }
}
