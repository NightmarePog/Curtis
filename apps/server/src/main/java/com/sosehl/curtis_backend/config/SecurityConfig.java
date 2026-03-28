package com.sosehl.curtis_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        OAuth2AuthorizedClientService clientService
    ) throws Exception {
        http
            .cors(cors -> {})
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers("/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            )
            .oauth2Login(oauth2 ->
                oauth2.successHandler(customAuthSuccessHandler(clientService))
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customAuthSuccessHandler(
        OAuth2AuthorizedClientService clientService
    ) {
        return (
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
        ) -> {
            OAuth2AuthenticationToken oauthToken =
                (OAuth2AuthenticationToken) authentication;
            OidcUser oidcUser = (OidcUser) oauthToken.getPrincipal();

            OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
            );

            String accessToken =
                client.getAccessToken() != null
                    ? client.getAccessToken().getTokenValue()
                    : "";
            String idToken =
                oidcUser.getIdToken() != null
                    ? oidcUser.getIdToken().getTokenValue()
                    : "";

            Map<String, Object> safeUserAttrs = new HashMap<>();
            if (oidcUser.getAttributes() != null) {
                oidcUser
                    .getAttributes()
                    .forEach((k, v) -> {
                        if (v != null) safeUserAttrs.put(k, v.toString());
                    });
            }

            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", accessToken);
            data.put("idToken", idToken);
            data.put("user", safeUserAttrs);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(response.getWriter(), data);
            response.getWriter().flush();
        };
    }
}
