package makeus.cmc.malmo.admin;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.out.jwt.TokenInfo;
import makeus.cmc.malmo.domain.model.member.Member;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/admin")
@RequiredArgsConstructor
@RestController
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/members")
    public List<Member> getMembers() {
        return adminService.getMembers();
    }

    @PostMapping("/login")
    public TokenInfo login(@RequestBody LoginRequest request) {
        return adminService.login(request.nickname, request.token);
    }

    @Data
    @NoArgsConstructor
    public static class LoginRequest {
        private String nickname;
        private String token;
    }
}
