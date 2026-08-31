package com.sosehl.curtis_backend.config;

import com.sosehl.curtis_backend.common.components.CustomOAuth2SuccessHandler;
import com.sosehl.curtis_backend.common.components.EntraOidcUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
                    .requestMatchers("/error", "/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/v1/quiz")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/v1/quiz/*/questions")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/v1/quiz")
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.PATCH, "/v1/quiz/*")
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.DELETE, "/v1/quiz/*")
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
                     .requestMatchers(
                         HttpMethod.GET,
                         "/v1/sessions/*/pending-text-answers"
                     )
                     .hasRole("TEACHER")
                     .requestMatchers(
                         HttpMethod.POST,
                         "/v1/sessions/*/text-answers/*/grade"
                     )
                     .hasRole("TEACHER")
                     .requestMatchers(HttpMethod.POST, "/v1/quiz/import")
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
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Unauthorized\"}");
                })
            );

        return http.build();
    }
}
