package com.sosehl.curtis.feature.sessions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sosehl.curtis.feature.sessions.attempt.AttemptQueryService;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminSessionControllerTest {

    private final SessionService sessions = mock(
        SessionService.class
    );
    private final AttemptQueryService attempts = mock(AttemptQueryService.class);
    private final CurrentUser administrator = new CurrentUser(
        UUID.randomUUID(),
        "https://issuer.example.test",
        "admin-subject",
        "admin@example.test",
        "Administrator",
        Set.of(UserRole.ADMINISTRATOR)
    );
    private AdminSessionController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminSessionController(sessions, attempts);
    }

    @Test
    void listsSessionsForResolvedAdministrator() {
        controller.sessions(administrator);

        verify(sessions).listForAdministrator();
    }

    @Test
    void closesSessionForResolvedAdministrator() {
        UUID sessionId = UUID.randomUUID();
        controller.close(sessionId, administrator);

        verify(sessions).closeAsAdministrator(sessionId);
    }
}
