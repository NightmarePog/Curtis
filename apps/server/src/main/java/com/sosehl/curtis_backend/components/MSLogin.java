package com.sosehl.curtis_backend.components;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MSLogin {

    private final String tenantId;
    private final String clientId;
    private final String redirectUri;

    public MSLogin(
        @Value("${microsoft.tenant-id}") String tenantId,
        @Value("${microsoft.client-id}") String clientId,
        @Value("${microsoft.redirect-uri}") String redirectUri
    ) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    // gettery

    public String getTenantId() {
        return tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
