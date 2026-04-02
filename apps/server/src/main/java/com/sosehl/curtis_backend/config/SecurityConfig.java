package com.sosehl.curtis_backend.config;

import com.sosehl.curtis_backend.common.components.CustomOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CustomOAuth2SuccessHandler successHandler
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
            .oauth2Login(oauth2 -> oauth2.successHandler(successHandler))
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
