package com.sosehl.curtis_backend.common.components;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomOAuth2SuccessHandler
    implements AuthenticationSuccessHandler
{

    private final String frontendUrl;

    public CustomOAuth2SuccessHandler(
        @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws java.io.IOException {
        response.sendRedirect(frontendUrl);
    }
}
