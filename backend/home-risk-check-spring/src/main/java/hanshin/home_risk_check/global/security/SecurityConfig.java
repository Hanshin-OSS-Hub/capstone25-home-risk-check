package hanshin.home_risk_check.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/*
 * Spring Security 기본 설정
 *
 * 현재는 인증 기능을 아직 붙이지 않았으므로
 * API 테스트가 가능하도록 /api/** 경로는 열어둔다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                /*
                 * REST API 서버라서 CSRF는 일단 비활성화
                 * 추후 JWT/세션 전략 확정되면 다시 조정 가능
                 */
                .csrf(csrf -> csrf.disable())

                /*
                 * /api/** 는 현재 테스트 가능하도록 허용
                 * 그 외 요청은 인증 필요
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                )

                /*
                 * 기본 로그인 폼 비활성화
                 */
                .formLogin(form -> form.disable());

        return http.build();
    }
}