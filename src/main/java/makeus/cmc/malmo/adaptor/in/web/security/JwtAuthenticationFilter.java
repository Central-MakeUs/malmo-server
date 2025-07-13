package makeus.cmc.malmo.adaptor.in.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import makeus.cmc.malmo.application.port.out.ValidateTokenPort;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final ValidateTokenPort validateTokenPort;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("[{}] [{} {}] REQUEST_ID : {}", LocalDateTime.now(), request.getMethod(), request.getRequestURI(), MDC.get("request_id"));

        String token = resolveToken(request);

        if (token != null && validateTokenPort.validateToken(token)) {
            try {
                String userId = validateTokenPort.getMemberIdFromToken(token);
                String role = validateTokenPort.getMemberRoleFromToken(token);

                if (userId != null && role != null) {
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                    UserDetails userDetails = User.builder()
                            .username(userId)
                            .password("")
                            .authorities(authority)
                            .build();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // JWT 파싱 에러 시 로그 출력
                log.error("JWT 토큰 파싱 중 오류 발생: ", e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        log.info("[{}] [{} {}] REQUEST_ID : {}", LocalDateTime.now(), request.getMethod(), request.getRequestURI(), MDC.get("request_id"));

        String path = request.getRequestURI();
        boolean shouldSkip = path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/login") ||
                path.equals("/refresh") ||
                path.equals("/test");
        
        return shouldSkip;
    }
}