package makeus.cmc.malmo.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.domain.model.love_type.LoveType;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeQuestion;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.model.terms.Terms;
import makeus.cmc.malmo.domain.value.type.LoveTypeQuestionType;
import makeus.cmc.malmo.domain.value.type.TermsType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Hidden
@RequestMapping("/admin")
@RequiredArgsConstructor
@RestController
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/login")
    public TokenInfo login(@RequestBody LoginRequest request) {
        return adminService.login(request.nickname, request.token);
    }

    @GetMapping("/members")
    public List<Member> getMembers() {
        return adminService.getMembers();
    }

    // 애착 유형, 애착 유형 질문 관련 API
    @GetMapping("/love-types")
    public List<LoveType> getLoveTypes() {
        return adminService.getLoveTypes();
    }

    @PatchMapping("/love-types/{loveTypeId}")
    public Long getLoveTypes(@PathVariable Long loveTypeId, @RequestBody UpdateLoveTypeRequest request) {
        return adminService.updateLoveType(loveTypeId, request.title, request.summary, request.content, request.imageUrl);
    }

    @GetMapping("/love-types/questions")
    public List<LoveTypeQuestion> getLoveTypeQuestions() {
        return adminService.getLoveTypeQuestions();
    }

    @PatchMapping("/love-types/questions/{questionId}")
    public Long updateLoveTypeQuestion(@RequestBody UpdateLoveTypeQuestionRequest request, @PathVariable Long questionId) {
        return adminService.updateLoveTypeQuestion(questionId, request.content, request.questionNumber, request.isReversed, request.loveTypeQuestionType);
    }

    // 약관 조회 및 수정 API
    @GetMapping("/terms")
    public List<Terms> getTerms() {
        return adminService.getTerms();
    }

    @DeleteMapping("/terms/{termsId}")
    public Long deleteTerms(@PathVariable Long termsId) {
        return adminService.deleteTerms(termsId);
    }

    @PostMapping("/terms")
    public Long createTerms(@RequestBody CreateTermsRequest request) {
        return adminService.createTerms(request.title, request.content, request.version, request.isRequired, request.termsType);
    }

    @Data
    @NoArgsConstructor
    public static class LoginRequest {
        private String nickname;
        private String token;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateLoveTypeRequest {
        private String title;
        private String summary;
        private String content;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    public static class UpdateLoveTypeQuestionRequest {
        private String content;
        private int questionNumber;
        private boolean isReversed;
        private LoveTypeQuestionType loveTypeQuestionType;
    }

    @Data
    @NoArgsConstructor
    public static class CreateTermsRequest {
        private String title;
        private String content;
        private float version;
        @JsonProperty("isRequired")
        private boolean isRequired;
        private TermsType termsType;
    }
}
