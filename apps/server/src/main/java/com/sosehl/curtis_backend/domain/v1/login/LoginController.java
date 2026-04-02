package com.sosehl.curtis_backend.domain.v1.login;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class LoginController {

    @GetMapping("/login")
    public RedirectView login() {
        return new RedirectView("/oauth2/authorization/microsoft");
    }
}
