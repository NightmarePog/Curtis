package com.sosehl.curtis.config;

import com.sosehl.curtis.platform.security.infrastructure.EntraOidcUserService;
import com.sosehl.curtis.shared.errors.ProblemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        EntraOidcUserService oidcUserService,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver problems,
        @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");

        http
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers(
                        "/login",
                        "/oauth2/**",
                        "/login/oauth2/**",
                        "/error",
                        "/actuator/health",
                        "/actuator/health/**"
                    )
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/v1/media/**")
                    .authenticated()
                    .requestMatchers("/openapi", "/openapi/**")
                    .hasRole("ADMINISTRATOR")
                    .requestMatchers("/v1/admin/**")
                    .hasRole("ADMINISTRATOR")
                    .requestMatchers("/v1/teacher/**")
                    .hasRole("TEACHER")
                    .requestMatchers("/v1/student/**")
                    .hasRole("STUDENT")
                    .requestMatchers("/v1/me", "/v1/events")
                    .authenticated()
                    .anyRequest()
                    .denyAll()
            )
            .oauth2Login(oauth2 ->
                oauth2
                    .defaultSuccessUrl(frontendUrl + "/dashboard", true)
                    .failureHandler((request, response, exception) -> {
                        LOGGER.error("Microsoft OAuth login failed", exception);
                        response.sendRedirect(frontendUrl + "/login?error=oauth");
                    })
                    .userInfoEndpoint(userInfo ->
                        userInfo.oidcUserService(oidcUserService)
                    )
            )
            .logout(logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler(
                        new HttpStatusReturningLogoutSuccessHandler(
                            HttpStatus.NO_CONTENT
                        )
                    )
            )
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .csrf(csrf ->
                csrf
                    .csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(csrfHandler)
            )
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint((request, response, exception) ->
                        problems.resolveException(
                            request,
                            response,
                            null,
                            ProblemException.unauthorized(
                                "authentication_required",
                                "Authentication is required."
                            )
                        )
                    )
                    .accessDeniedHandler((request, response, exception) ->
                        problems.resolveException(
                            request,
                            response,
                            null,
                            ProblemException.forbidden(
                                exception instanceof CsrfException
                                    ? "csrf_invalid"
                                    : "access_denied",
                                exception instanceof CsrfException
                                    ? "A valid CSRF token is required."
                                    : "You are not allowed to perform this action."
                            )
                        )
                    )
            );

        return http.build();
    }
}
