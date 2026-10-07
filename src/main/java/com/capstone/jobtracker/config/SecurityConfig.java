package com.capstone.jobtracker.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authProvider())
                .authorizeHttpRequests(auth -> auth
                        // 로그인 없이 접근 가능한 경로
                        .requestMatchers(
                                "/", "/home", "/login", "/register", "/register/form",
                                "/css/**", "/js/**", "/images/**",
                                "/api/users/register", "/error"
                        ).permitAll()

                        // 관리자 전용 URL 예시 (필요하면 사용)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 지원현황 등 나머지 업무 화면은 로그인 필요
                        .requestMatchers("/applications/**").authenticated()
                        .requestMatchers("/interviews/**").authenticated()
                        .requestMatchers("/resumes/**").authenticated()
                        .requestMatchers("/specs/**").authenticated()
                        .requestMatchers("/coverletters/**").authenticated()

                        // 그 외 나머지는 일단 전부 로그인 필요
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
