package makeus.cmc.malmo.config;

import lombok.RequiredArgsConstructor;
import makeus.cmc.malmo.adaptor.in.web.security.CustomAccessDeniedHandler;
import makeus.cmc.malmo.adaptor.in.web.security.CustomAuthenticationEntryPoint;
import makeus.cmc.malmo.adaptor.in.web.security.JwtAuthenticationFilter;
import makeus.cmc.malmo.adaptor.out.jwt.JwtAdaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAdaptor jwtAdaptor;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Value("${security.client.url.production}")
    private String PRODUCTION_CLIENT_URL;

    @Value("${security.client.url.development}")
    private String DEVELOPMENT_CLIENT_URL;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/login/**", "/refresh", "/test").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", 
                                        "/v3/api-docs/**", "/v3/api-docs", "/webjars/**").permitAll()
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 여기에서 허용할 도메인만 설정
        config.setAllowedOrigins(List.of(PRODUCTION_CLIENT_URL, DEVELOPMENT_CLIENT_URL));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
