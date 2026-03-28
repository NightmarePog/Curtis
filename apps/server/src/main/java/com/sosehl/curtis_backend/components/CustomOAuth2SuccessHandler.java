package com.sosehl.curtis_backend.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomOAuth2SuccessHandler
    implements AuthenticationSuccessHandler
{

    private final OAuth2AuthorizedClientService clientService;

    public CustomOAuth2SuccessHandler(
        OAuth2AuthorizedClientService clientService
    ) {
        this.clientService = clientService;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws java.io.IOException {
        if (
            !(authentication instanceof OAuth2AuthenticationToken oauthToken) ||
            !(oauthToken.getPrincipal() instanceof OidcUser oidcUser)
        ) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(),
            oauthToken.getName()
        );

        String accessToken =
            client != null && client.getAccessToken() != null
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
        new ObjectMapper().writeValue(response.getWriter(), data);
        response.getWriter().flush();
    }
}
