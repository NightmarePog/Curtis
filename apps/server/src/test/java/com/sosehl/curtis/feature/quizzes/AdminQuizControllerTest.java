package com.sosehl.curtis.feature.quizzes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminQuizControllerTest {

    private final QuizService quizzes = mock(
        QuizService.class
    );
    private final UserDirectory users = mock(UserDirectory.class);
    private final CurrentUser administrator = new CurrentUser(
        UUID.randomUUID(),
        "https://issuer.example.test",
        "admin-subject",
        "admin@example.test",
        "Administrator",
        Set.of(UserRole.ADMINISTRATOR)
    );
    private AdminQuizController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminQuizController(quizzes, users);
    }

    @Test
    void listsQuizzesForResolvedAdministrator() {
        controller.list(administrator);

        verify(quizzes).listForAdministrator();
    }
}
