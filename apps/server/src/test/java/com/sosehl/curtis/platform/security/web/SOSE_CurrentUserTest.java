package com.sosehl.curtis.platform.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.platform.security.application.CurrentUserService;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.context.request.ServletWebRequest;

class SOSE_CurrentUserTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesVerifiedCurtisUserThroughCurrentUserService() throws Exception {
        OidcUser oidcUser = mock(OidcUser.class);
        CurrentUser expected = new CurrentUser(
            UUID.randomUUID(),
            "https://issuer.example.test",
            "teacher-subject",
            "teacher@example.test",
            "Test Teacher",
            Set.of(UserRole.TEACHER)
        );
        CurrentUserService currentUsers = mock(CurrentUserService.class);
        when(currentUsers.require(oidcUser)).thenReturn(expected);

        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton(
            "currentUserService",
            currentUsers
        );
        AuthenticationPrincipalArgumentResolver resolver =
            new AuthenticationPrincipalArgumentResolver();
        resolver.setBeanResolver(new BeanFactoryResolver(context));
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(oidcUser, null, List.of())
        );

        Object resolved = resolver.resolveArgument(
            currentUserParameter(),
            null,
            new ServletWebRequest(new MockHttpServletRequest()),
            null
        );

        assertThat(resolved).isSameAs(expected);
        verify(currentUsers).require(oidcUser);
        context.close();
    }

    private MethodParameter currentUserParameter() throws NoSuchMethodException {
        Method method = Probe.class.getDeclaredMethod(
            "handle",
            CurrentUser.class
        );
        return new MethodParameter(method, 0);
    }

    private static final class Probe {
        void handle(@SOSE_CurrentUser CurrentUser currentUser) {}
    }
}
