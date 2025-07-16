package makeus.cmc.malmo.adaptor.in.web.docs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import makeus.cmc.malmo.domain.value.state.MemberState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    }

    // 로그인 관련 응답
    @Getter
    @Schema(description = "로그인 성공 응답")
    public static class LoginSuccessResponse extends BaseSwaggerResponse<LoginData> {
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
    @Schema(description = "멤버 초대 코드 성공 응답")
    public static class GetInviteCodeSuccessResponse extends BaseSwaggerResponse<InviteCodeResponseData> {
    }

    // 커플 관련 응답
    @Getter
    @Schema(description = "커플 연결 성공 응답")
    public static class CoupleLinkSuccessResponse extends BaseSwaggerResponse<CoupleLinkData> {
    }

    @Getter
    @Schema(description = "커플 연결 끊기 성공 응답")
    public static class CoupleUnlinkSuccessResponse extends BaseSwaggerResponse<CoupleUnlinkData> {
    }

    // 애착유형 관련 응답
    @Getter
    @Schema(description = "애착유형 질문 조회 성공 응답")
    public static class LoveTypeQuestionSuccessResponse extends BaseSwaggerResponse<BaseListSwaggerResponse<LoveTypeQuestionData>> {
    }

    @Getter
    @Schema(description = "애착유형 등록 성공 응답")
    public static class RegisterLoveTypeSuccessResponse extends BaseSwaggerResponse<RegisterLoveTypeData> {
    }

    @Getter
    @Schema(description = "애착유형 조회 성공 응답")
    public static class GetLoveTypeSuccessResponse extends BaseSwaggerResponse<GetLoveTypeData> {
    }

    // 질문 관련 응답
    @Getter
    @Schema(description = "오늘의 질문 조회 성공 응답")
    public static class QuestionSuccessResponse extends BaseSwaggerResponse<QuestionData> {
    }

    @Getter
    @Schema(description = "질문 리스트 조회 성공 응답")
    public static class QuestionListSuccessResponse extends BaseSwaggerResponse<BaseListSwaggerResponse<QuestionListData>> {
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

        @Schema(description = "연애 시작일", example = "2023-01-15")
        private LocalDate loveStartDate;

        @Schema(description = "애착 유형 ID", example = "1")
        private Long loveTypeId;

        @Schema(description = "애착 유형 제목", example = "안정형")
        private String loveTypeTitle;

        @Schema(description = "회피 비율", example = "0.3")
        private float avoidanceRate;

        @Schema(description = "불안 비율", example = "0.2")
        private float anxietyRate;

        @Schema(description = "닉네임", example = "홍길동")
        private String nickname;

        @Schema(description = "이메일", example = "test@example.com")
        private String email;
    }

    @Getter
    @Schema(description = "파트너 멤버 정보 응답 데이터")
    public static class PartnerMemberData {
        @Schema(description = "멤버 상태", example = "ALIVE")
        private MemberState memberState;

        @Schema(description = "애착 유형 ID", example = "1")
        private Long loveTypeId;

        @Schema(description = "애착 유형 제목", example = "안정형")
        private String loveTypeTitle;

        @Schema(description = "회피 비율", example = "0.3")
        private float avoidanceRate;

        @Schema(description = "불안 비율", example = "0.2")
        private float anxietyRate;

        @Schema(description = "닉네임", example = "김영희")
        private String nickname;
    }

    @Getter
    @Schema(description = "멤버 정보 수정 응답 데이터")
    public static class UpdateMemberData {
        @Schema(description = "닉네임", example = "홍길동")
        private String nickname;

        @Schema(description = "이메일", example = "test@example.com")
        private String email;
    }

    @Getter
    @Schema(description = "멤버 탈퇴 응답 데이터")
    public static class DeleteMemberData {
        @Schema(description = "탈퇴한 멤버 ID", example = "1")
        private Long memberId;
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
    @Schema(description = "커플 연결 응답 데이터")
    public static class CoupleLinkData {
        @Schema(description = "생성된 커플 ID", example = "1")
        private Long coupleId;
    }

    @Getter
    @Schema(description = "커플 연결 끊기 응답 데이터")
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
    @Schema(description = "애착유형 등록 응답 데이터")
    public static class RegisterLoveTypeData {
        @Schema(description = "애착 유형 ID", example = "1")
        private Long loveTypeId;

        @Schema(description = "애착 유형", example = "안정형")
        private String title;

        @Schema(description = "애착유형 요약", example = "안정적인 유형")
        private String summary;

        @Schema(description = "애착유형 설명", example = "안정된 애착 관계를 형성하는 유형입니다.")
        private String content;

        @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
        private String imageUrl;
    }

    @Getter
    @Schema(description = "애착유형 조회 응답 데이터")
    public static class GetLoveTypeData {

        @Schema(description = "애착 유형 ID", example = "1")
        private Long loveTypeId;

        @Schema(description = "애착 유형", example = "안정형")
        private String title;

        @Schema(description = "애착유형 요약", example = "안정적인 유형")
        private String summary;

        @Schema(description = "애착유형 설명", example = "안정된 애착 관계를 형성하는 유형입니다.")
        private String content;

        @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
        private String imageUrl;
    }

    @Getter
    @Schema(description = "질문 응답 데이터")
    public static class QuestionData {
        @Schema(description = "커플 질문 ID", example = "1")
        private Long coupleQuestionId;

        @Schema(description = "질문 제목", example = "오늘 하루 어땠나요?")
        private String title;

        @Schema(description = "질문 내용", example = "오늘 하루 중 가장 기억에 남는 순간은 무엇인가요?")
        private String content;
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
        @Schema(description = "멤버 답변 ID", example = "1")
        private Long memberAnswerId;
    }

    @Getter
    @Schema(description = "과거 질문 조회 응답 데이터")
    public static class PastQuestionData {
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
    @Schema(description = "과거 답변 조회 응답 데이터")
    public static class PastAnswerData {
        @Schema(description = "커플 질문 ID", example = "1")
        private Long coupleQuestionId;

        @Schema(description = "질문 제목", example = "오늘 하루 어땠나요?")
        private String title;

        @Schema(description = "질문 내용", example = "오늘 하루 중 가장 기억에 남는 순간은 무엇인가요?")
        private String content;

        @Schema(description = "생성일시", example = "2023-07-03T10:00:00")
        private LocalDateTime createdAt;

        @Schema(description = "답변 리스트")
        private List<PastAnswerDto> answers;
    }

    @Getter
    @Schema(description = "과거 답변 상세 데이터")
    public static class PastAnswerDto {
        @Schema(description = "멤버 답변 ID", example = "1")
        private Long memberAnswerId;

        @Schema(description = "답변 내용", example = "오늘은 정말 좋은 하루였어요!")
        private String answer;

        @Schema(description = "답변한 멤버 닉네임", example = "홍길동")
        private String memberNickname;

        @Schema(description = "답변 생성일시", example = "2023-07-03T10:00:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Schema(description = "약관 응답 데이터")
    public static class TermsResponseData {
        @Schema(description = "약관 ID", example = "1")
        private Long termsId;

        @Schema(description = "약관 제목", example = "서비스 이용약관")
        private String title;

        @Schema(description = "약관 내용", example = "본 약관은...")
        private String content;

        @Schema(description = "필수 동의 여부", example = "true")
        private Boolean isRequired;

        @Schema(description = "생성일시", example = "2023-07-03T10:00:00")
        private LocalDateTime createdAt;
    }

    @Getter
    @Schema(description = "초대 코드 응답 데이터")
    public static class InviteCodeResponseData {
        private String coupleCode;
    }

    @Getter
    @Schema(description = "채팅 응답 데이터")
    public static class SendChatData {
        @Schema(description = "사용자가 보낸 메시지의 ID", example = "1")
        private Long messageId;
    }
}