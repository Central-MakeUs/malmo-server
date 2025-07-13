package makeus.cmc.malmo.config;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.web.security.CustomAccessDeniedHandler;
import makeus.cmc.malmo.adaptor.in.web.security.CustomAuthenticationEntryPoint;
import makeus.cmc.malmo.adaptor.in.web.security.JwtAuthenticationFilter;
import makeus.cmc.malmo.adaptor.out.jwt.JwtAdaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAdaptor jwtAdaptor;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login/**", "/refresh", "/test").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", 
                                        "/v3/api-docs/**", "/v3/api-docs", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/admin/**").permitAll()
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint) // 401 처리
                        .accessDeniedHandler(accessDeniedHandler)) // 403 처리
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtAdaptor),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
