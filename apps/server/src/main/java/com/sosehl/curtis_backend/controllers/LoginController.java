package com.sosehl.curtis_backend.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class LoginController {

    @GetMapping("/login")
    public RedirectView login() {
        // přesměruje přímo na Microsoft OAuth2 authorization endpoint
        return new RedirectView("/oauth2/authorization/microsoft");
    }
}
