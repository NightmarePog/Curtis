package com.sosehl.curtis.platform.security.api;

import com.sosehl.curtis.platform.security.api.dto.MeResponse;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
public class MeController {
    @GetMapping
    MeResponse me(
        @SOSE_CurrentUser CurrentUser user,
        @Parameter(hidden = true) CsrfToken csrfToken
    ) {
        csrfToken.getToken();
        return new MeResponse(
            user.id(),
            user.subject(),
            user.username(),
            user.displayName(),
            user.roles()
        );
    }
}
