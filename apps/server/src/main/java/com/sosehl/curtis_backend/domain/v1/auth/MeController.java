package com.sosehl.curtis_backend.domain.v1.auth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
public class MeController {

    @GetMapping
    public Map<String, Object> me(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sub", authentication.getName());

        List<String> roles = authentication
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring("ROLE_".length()))
            .sorted()
            .toList();
        body.put("roles", roles);

        return body;
    }
}
