package com.sosehl.curtis_backend.services;

import com.sosehl.curtis_backend.components.MSLogin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    @Autowired
    private MSLogin mslogin;

    @Bean
    public String login() {
        String LoginUrl =
            "https://login.microsoftonline.com/" +
            mslogin.getTenantId() +
            "/oauth2/v2.0/authorize?client_id=" +
            mslogin.getClientId() +
            "&response_type=code&redirect_uri=" +
            mslogin.getRedirectUri() +
            "&response_mode=query&scope=openid%20profile%20email";

        return LoginUrl;
    }
}
