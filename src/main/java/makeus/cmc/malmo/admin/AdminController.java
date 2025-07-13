package makeus.cmc.malmo.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.domain.model.love_type.LoveType;
import makeus.cmc.malmo.domain.model.love_type.LoveTypeQuestion;
import makeus.cmc.malmo.domain.model.member.Member;
import makeus.cmc.malmo.domain.value.type.LoveTypeQuestionType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
