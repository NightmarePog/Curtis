package com.sosehl.curtis_backend.config;

import com.sosehl.curtis_backend.common.components.CustomOAuth2SuccessHandler;
import com.sosehl.curtis_backend.common.components.EntraOidcUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final String frontendUrl;

    public SecurityConfig(
        @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CustomOAuth2SuccessHandler successHandler,
        EntraOidcUserService entraOidcUserService
    ) throws Exception {
        http
            .cors(cors -> {})
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/v1/quiz")
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.GET, "/v1/quiz/*")
                    .hasRole("TEACHER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/v1/quiz/*/questions"
                    )
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.POST, "/v1/quiz")
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.PATCH, "/v1/quiz/*")
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.POST, "/v1/quiz/*/questions")
                    .hasRole("TEACHER")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/v1/quiz/*/questions/*"
                    )
                    .hasRole("TEACHER")
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/v1/quiz/*/questions/*"
                    )
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.POST, "/v1/sessions")
                    .hasRole("TEACHER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/v1/sessions/*/results"
                    )
                    .hasRole("TEACHER")
                    .anyRequest()
                    .authenticated()
            )
            .oauth2Login(oauth2 ->
                oauth2
                    .successHandler(successHandler)
                    .userInfoEndpoint(userInfo ->
                        userInfo.oidcUserService(entraOidcUserService)
                    )
            )
            .logout(logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl(frontendUrl)
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
