# Backend Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add teacher/student role enforcement, server-side per-question time-limit enforcement, and a no-auth drop-folder YAML quiz importer to the `apps/server` Spring Boot backend.

**Architecture:** (1) A custom `OidcUserService` maps an Entra ID `roles` claim onto Spring Security authorities, which `SecurityConfig` uses to gate quiz-mutating endpoints behind `ROLE_TEACHER`. (2) `StudentAttempt` gains an injectable `Clock` and timestamps each served question, silently discarding answers submitted after `timeInSeconds` + a fixed grace period. (3) A `@Scheduled` `QuizYamlImportService` polls a local directory for `.yaml`/`.yml` files, parses+validates+imports them directly via `QuizRepository` (no HTTP surface, no auth — filesystem access to the host is the trust boundary), and files each result into `processed/` or `failed/`.

**Tech Stack:** Java 17, Spring Boot 3.4.3, Spring Security (OAuth2/OIDC), Spring Data JPA, Jackson (`jackson-dataformat-yaml`), Lombok, JUnit 5 + AssertJ + MockMvc, H2 (test DB).

**Spec:** `docs/superpowers/specs/2026-07-26-backend-hardening-design.md`

All paths below are relative to `apps/server/`.

---

### Task 1: Entra role → Spring Security authority mapping

**Files:**
- Create: `src/main/java/com/sosehl/curtis/common/components/EntraRoleMapper.java`
- Test: `src/test/java/com/sosehl/curtis/security/EntraRoleMapperTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.sosehl.curtis.security;

import static org.assertj.core.api.Assertions.*;

import com.sosehl.curtis.common.components.EntraRoleMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class EntraRoleMapperTest {

    @Test
    void shouldReturnEmptySetForNullInput() {
        assertThat(EntraRoleMapper.mapRoles(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptySetForEmptyInput() {
        assertThat(EntraRoleMapper.mapRoles(List.of())).isEmpty();
    }

    @Test
    void shouldMapTeacherRole() {
        Set<GrantedAuthority> authorities = EntraRoleMapper.mapRoles(
            List.of("Teacher")
        );
        assertThat(authorities).containsExactly(
            new SimpleGrantedAuthority("ROLE_TEACHER")
        );
    }

    @Test
    void shouldMapStudentRole() {
        Set<GrantedAuthority> authorities = EntraRoleMapper.mapRoles(
            List.of("Student")
        );
        assertThat(authorities).containsExactly(
            new SimpleGrantedAuthority("ROLE_STUDENT")
        );
    }

    @Test
    void shouldMapBothRolesWhenPresent() {
        Set<GrantedAuthority> authorities = EntraRoleMapper.mapRoles(
            List.of("Teacher", "Student")
        );
        assertThat(authorities).containsExactlyInAnyOrder(
            new SimpleGrantedAuthority("ROLE_TEACHER"),
            new SimpleGrantedAuthority("ROLE_STUDENT")
        );
    }

    @Test
    void shouldIgnoreUnknownRoleValues() {
        Set<GrantedAuthority> authorities = EntraRoleMapper.mapRoles(
            List.of("SchoolAdmin")
        );
        assertThat(authorities).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.sosehl.curtis.security.EntraRoleMapperTest"`
Expected: FAIL — compilation error, `EntraRoleMapper` does not exist.

- [ ] **Step 3: Write the implementation**

```java
package com.sosehl.curtis.common.components;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class EntraRoleMapper {

    private EntraRoleMapper() {}

    public static Set<GrantedAuthority> mapRoles(Collection<String> roles) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        if (roles == null) {
            return authorities;
        }

        for (String role : roles) {
            if ("Teacher".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));
            } else if ("Student".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
            }
        }
        return authorities;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.sosehl.curtis.security.EntraRoleMapperTest"`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sosehl/curtis/common/components/EntraRoleMapper.java src/test/java/com/sosehl/curtis/security/EntraRoleMapperTest.java
git commit -m "feat(security): map Entra roles claim to Spring Security authorities"
```

---

### Task 2: Custom OIDC user service that attaches role authorities

**Files:**
- Create: `src/main/java/com/sosehl/curtis/common/components/EntraOidcUserService.java`

No dedicated unit test for this class: it is a thin wrapper that delegates to Spring's own `OidcUserService` (which performs real network calls to the userinfo endpoint), and the only real logic it adds — string-to-authority mapping — is already fully covered by `EntraRoleMapperTest`. Its wiring is verified end-to-end by the authorization tests in Task 3.

- [ ] **Step 1: Write the implementation**

```java
package com.sosehl.curtis.common.components;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class EntraOidcUserService
    implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
        throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        List<String> roles = oidcUser
            .getIdToken()
            .getClaimAsStringList("roles");

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(
            oidcUser.getAuthorities()
        );
        authorities.addAll(EntraRoleMapper.mapRoles(roles));

        return new DefaultOidcUser(
            authorities,
            oidcUser.getIdToken(),
            oidcUser.getUserInfo(),
            "sub"
        );
    }
}
```

- [ ] **Step 2: Compile to verify no errors**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/sosehl/curtis/common/components/EntraOidcUserService.java
git commit -m "feat(security): wrap OidcUserService to attach Entra role authorities"
```

---

### Task 3: Gate quiz-mutating endpoints behind ROLE_TEACHER

**Files:**
- Modify: `src/main/java/com/sosehl/curtis/config/SecurityConfig.java`
- Modify: `src/test/java/com/sosehl/curtis/quiz/QuizControllerTest.java`
- Create: `src/test/java/com/sosehl/curtis/security/RoleAuthorizationTest.java`

- [ ] **Step 1: Update `QuizControllerTest` to grant `ROLE_TEACHER` where the endpoint now requires it**

In `src/test/java/com/sosehl/curtis/quiz/QuizControllerTest.java`, change the `setUp()` method's request builder from:

```java
                    .with(SecurityMockMvcRequestPostProcessors.user("testuser"))
```

to:

```java
                    .with(
                        SecurityMockMvcRequestPostProcessors
                            .user("testuser")
                            .roles("TEACHER")
                    )
```

Change these five test method annotations from `@WithMockUser` to `@WithMockUser(authorities = "ROLE_TEACHER")`:
- `shouldCreateQuiz`
- `shouldFailCreateWhenTitleIsMissing`
- `shouldFailCreateWhenTitleIsTooLong`
- `shouldPatchQuizTitle`
- `shouldPatchOnlyProvidedFields`
- `shouldReturnNotFoundWhenPatchingUnknownQuiz`

Leave `shouldGetQuizByUuid`, `shouldReturnNotFoundForUnknownUuid`, `shouldReturnAllQuizzes`, and the three `shouldRedirect*`/`shouldFailCreateWhenNotAuthenticated` tests untouched — reads stay open to any authenticated user, and unauthenticated requests are unaffected by role changes.

- [ ] **Step 2: Run the updated test file as a sanity check**

Run: `./gradlew test --tests "com.sosehl.curtis.quiz.QuizControllerTest"`
Expected: PASS. Granting `ROLE_TEACHER` doesn't break anything yet because `SecurityConfig` doesn't require any specific role yet — every request is still just `authenticated()`. This step only confirms the test file itself compiles and the role authority was added correctly. The actual red step comes from the new negative test in Step 3.

- [ ] **Step 3: Write the new `RoleAuthorizationTest` (this is the test that proves the gate doesn't exist yet)**

```java
package com.sosehl.curtis.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis.domain.v1.question.dto.QuestionCreateDto;
import com.sosehl.curtis.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis.domain.v1.quiz.dto.QuizPatchRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class RoleAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID existingQuizUuid;

    @BeforeEach
    void setUp() throws Exception {
        QuizCreateRequest request = new QuizCreateRequest();
        request.setTitle("Role Test Quiz");
        request.setMaxQuestionsPerSession(5);
        request.setShuffle(false);

        MvcResult result = mockMvc
            .perform(
                post("/v1/quiz")
                    .with(
                        SecurityMockMvcRequestPostProcessors
                            .user("teacher1")
                            .roles("TEACHER")
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated())
            .andReturn();

        existingQuizUuid = objectMapper
            .readTree(result.getResponse().getContentAsString())
            .get("quizUuid")
            .traverse(objectMapper)
            .readValueAs(UUID.class);
    }

    @Test
    void shouldForbidQuizCreateWithoutTeacherRole() throws Exception {
        QuizCreateRequest request = new QuizCreateRequest();
        request.setTitle("Student Attempt");
        request.setMaxQuestionsPerSession(5);

        mockMvc
            .perform(
                post("/v1/quiz")
                    .with(SecurityMockMvcRequestPostProcessors.user("student1"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidQuizPatchWithoutTeacherRole() throws Exception {
        QuizPatchRequest patch = new QuizPatchRequest();
        patch.setTitle("Hacked title");

        mockMvc
            .perform(
                patch("/v1/quiz/" + existingQuizUuid)
                    .with(SecurityMockMvcRequestPostProcessors.user("student1"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patch))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidQuestionCreateWithoutTeacherRole() throws Exception {
        QuestionAnswer correct = new QuestionAnswer();
        correct.setAnswer("Java");
        correct.setIsCorrect(true);

        QuestionCreateDto dto = new QuestionCreateDto();
        dto.setQuestion("Co je Java?");
        dto.setAnswers(List.of(correct));
        dto.setTimeInSeconds(10);

        mockMvc
            .perform(
                post("/v1/quiz/" + existingQuizUuid + "/questions")
                    .with(SecurityMockMvcRequestPostProcessors.user("student1"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidSessionCreateWithoutTeacherRole() throws Exception {
        mockMvc
            .perform(
                post("/v1/sessions")
                    .with(SecurityMockMvcRequestPostProcessors.user("student1"))
                    .param("quizUuid", existingQuizUuid.toString())
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowQuizCreateWithTeacherRole() throws Exception {
        QuizCreateRequest request = new QuizCreateRequest();
        request.setTitle("Teacher Created Quiz");
        request.setMaxQuestionsPerSession(5);

        mockMvc
            .perform(
                post("/v1/quiz")
                    .with(
                        SecurityMockMvcRequestPostProcessors
                            .user("teacher2")
                            .roles("TEACHER")
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated());
    }
}
```

- [ ] **Step 4: Run `RoleAuthorizationTest` to verify the forbid-tests fail**

Run: `./gradlew test --tests "com.sosehl.curtis.security.RoleAuthorizationTest"`
Expected: FAIL — `shouldForbidQuizCreateWithoutTeacherRole`, `shouldForbidQuizPatchWithoutTeacherRole`, `shouldForbidQuestionCreateWithoutTeacherRole`, `shouldForbidSessionCreateWithoutTeacherRole` all get `201`/`200` instead of `403` because no role gate exists yet.

- [ ] **Step 5: Update `SecurityConfig` to require `ROLE_TEACHER` and wire in `EntraOidcUserService`**

Replace the full contents of `src/main/java/com/sosehl/curtis/config/SecurityConfig.java` with:

```java
package com.sosehl.curtis.config;

import com.sosehl.curtis.common.components.CustomOAuth2SuccessHandler;
import com.sosehl.curtis.common.components.EntraOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CustomOAuth2SuccessHandler successHandler,
        EntraOidcUserService entraOidcUserService
    ) throws Exception {
        http
            .cors(cors -> {})
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/v1/quiz")
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.PATCH, "/v1/quiz/*")
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.POST, "/v1/quiz/*/questions")
                    .hasRole("TEACHER")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/v1/quiz/*/questions/*"
                    )
                    .hasRole("TEACHER")
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/v1/quiz/*/questions/*"
                    )
                    .hasRole("TEACHER")
                    .requestMatchers(HttpMethod.POST, "/v1/sessions")
                    .hasRole("TEACHER")
                    .anyRequest()
                    .authenticated()
            )
            .oauth2Login(oauth2 ->
                oauth2
                    .successHandler(successHandler)
                    .userInfoEndpoint(userInfo ->
                        userInfo.oidcUserService(entraOidcUserService)
                    )
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
```

- [ ] **Step 6: Run both test files to verify everything passes**

Run: `./gradlew test --tests "com.sosehl.curtis.quiz.QuizControllerTest" --tests "com.sosehl.curtis.security.RoleAuthorizationTest"`
Expected: PASS (all tests in both files)

- [ ] **Step 7: Run the full test suite to check for unrelated regressions**

Run: `./gradlew test`
Expected: PASS. If `SessionTest` or `StudentAttemptTest` fail, they don't touch HTTP/security and shouldn't be affected — investigate only if something unexpected breaks.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/sosehl/curtis/config/SecurityConfig.java src/test/java/com/sosehl/curtis/quiz/QuizControllerTest.java src/test/java/com/sosehl/curtis/security/RoleAuthorizationTest.java
git commit -m "feat(security): require ROLE_TEACHER for quiz/question/session mutation endpoints"
```

---

### Task 4: Server-side per-question time-limit enforcement

**Files:**
- Modify: `src/main/java/com/sosehl/curtis/domain/v1/session/StudentAttempt.java`
- Modify: `src/test/java/com/sosehl/curtis/studentAttempt/StudentAttemptTest.java`

- [ ] **Step 1: Add failing tests to `StudentAttemptTest`**

Add these imports to the top of `src/test/java/com/sosehl/curtis/studentAttempt/StudentAttemptTest.java` (alongside the existing ones):

```java
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
```

Add this private static nested class and helper method inside the `StudentAttemptTest` class body (alongside the existing `attempt`/`setUp` members):

```java
    private static class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private QuestionResponse timedQuestion(int timeInSeconds) {
        QuestionAnswer correct = new QuestionAnswer();
        correct.setAnswer("Java");
        correct.setIsCorrect(true);

        QuestionAnswer wrong = new QuestionAnswer();
        wrong.setAnswer("Python");
        wrong.setIsCorrect(false);

        QuestionResponse question = new QuestionResponse();
        question.setQuestion("Co je Java?");
        question.setAnswers(List.of(correct, wrong));
        question.setTimeInSeconds(timeInSeconds);
        return question;
    }
```

Add these three test methods to the class:

```java
    @Test
    void shouldRecordAnswerWithinTimeLimit() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        StudentAttempt timedAttempt = new StudentAttempt(
            "student1",
            List.of(timedQuestion(10)),
            clock
        );

        timedAttempt.nextQuestion();
        clock.advance(Duration.ofSeconds(5));
        timedAttempt.addAnswer(List.of(0));

        assertThat(timedAttempt.calculateScore()).isEqualTo(1);
    }

    @Test
    void shouldDiscardAnswerSubmittedAfterGracePeriod() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        StudentAttempt timedAttempt = new StudentAttempt(
            "student1",
            List.of(timedQuestion(10)),
            clock
        );

        timedAttempt.nextQuestion();
        clock.advance(Duration.ofSeconds(13)); // 10s limit + 2s grace = 12s allowed
        timedAttempt.addAnswer(List.of(0)); // correct answer, but too late

        assertThat(timedAttempt.calculateScore()).isEqualTo(0);
    }

    @Test
    void shouldAcceptAnswerExactlyAtGraceBoundary() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        StudentAttempt timedAttempt = new StudentAttempt(
            "student1",
            List.of(timedQuestion(10)),
            clock
        );

        timedAttempt.nextQuestion();
        clock.advance(Duration.ofSeconds(12)); // exactly 10s + 2s grace
        timedAttempt.addAnswer(List.of(0));

        assertThat(timedAttempt.calculateScore()).isEqualTo(1);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.sosehl.curtis.studentAttempt.StudentAttemptTest"`
Expected: FAIL — compilation error, no `StudentAttempt(String, List, Clock)` constructor exists yet.

- [ ] **Step 3: Implement time-limit enforcement in `StudentAttempt`**

Replace the full contents of `src/main/java/com/sosehl/curtis/domain/v1/session/StudentAttempt.java` with:

```java
package com.sosehl.curtis.domain.v1.session;

import com.sosehl.curtis.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis.domain.v1.session.exceptions.NoMoreQuestionsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class StudentAttempt {

    private static final int GRACE_PERIOD_SECONDS = 2;

    private final String studentId;
    private final List<QuestionResponse> questions;
    private final List<List<Integer>> answers = new ArrayList<>();
    private final Clock clock;
    private int questionIndex = 0;
    private SessionStatus status = SessionStatus.RUNNING;
    private Instant servedAt;

    public StudentAttempt(String studentId, List<QuestionResponse> questions) {
        this(studentId, questions, Clock.systemDefaultZone());
    }

    public StudentAttempt(
        String studentId,
        List<QuestionResponse> questions,
        Clock clock
    ) {
        this.studentId = studentId;
        this.questions = questions;
        this.clock = clock;
    }

    public String getStudentId() {
        return studentId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public List<QuestionResponse> getQuestions() {
        return questions;
    }

    public List<List<Integer>> getAnswers() {
        return answers;
    }

    public QuestionResponse nextQuestion() {
        if (status != SessionStatus.RUNNING) {
            throw new IllegalStateException("Pokus již byl ukončen");
        }
        if (questionIndex >= questions.size()) {
            this.status = SessionStatus.ARCHIVED;
            throw new NoMoreQuestionsException("Žádné další otázky");
        }

        QuestionResponse question = questions.get(questionIndex);
        questionIndex++;
        servedAt = clock.instant();

        if (questionIndex >= questions.size()) {
            this.status = SessionStatus.ARCHIVED;
        }

        return question;
    }

    public void addAnswer(List<Integer> answer) {
        if (isAnswerLate()) {
            answers.add(new ArrayList<>());
            return;
        }
        answers.add(answer);
    }

    private boolean isAnswerLate() {
        if (servedAt == null || questionIndex == 0) {
            return false;
        }

        Integer timeInSeconds = questions
            .get(questionIndex - 1)
            .getTimeInSeconds();
        if (timeInSeconds == null) {
            return false;
        }

        Duration allowed = Duration.ofSeconds(
            timeInSeconds + GRACE_PERIOD_SECONDS
        );
        Duration elapsed = Duration.between(servedAt, clock.instant());
        return elapsed.compareTo(allowed) > 0;
    }

    public StudentAttempt finish() {
        this.status = SessionStatus.ARCHIVED;
        return this;
    }

    public int calculateScore() {
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            List<QuestionAnswer> answers = questions.get(i).getAnswers();
            List<Integer> correct = new ArrayList<>();
            for (int j = 0; j < answers.size(); j++) {
                if (
                    Boolean.TRUE.equals(answers.get(j).getIsCorrect())
                ) correct.add(j);
            }
            List<Integer> userAnswer =
                this.answers.size() > i
                    ? this.answers.get(i)
                    : new ArrayList<>();
            if (
                new HashSet<>(correct).equals(new HashSet<>(userAnswer))
            ) score++;
        }
        return score;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.sosehl.curtis.studentAttempt.StudentAttemptTest"`
Expected: PASS (8 tests: 5 existing + 3 new)

- [ ] **Step 5: Run the full test suite to check for regressions**

Run: `./gradlew test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sosehl/curtis/domain/v1/session/StudentAttempt.java src/test/java/com/sosehl/curtis/studentAttempt/StudentAttemptTest.java
git commit -m "feat(session): enforce per-question time limit server-side"
```

---

### Task 5: YAML import DTOs

**Files:**
- Create: `src/main/java/com/sosehl/curtis/domain/v1/quiz/dto/QuizYamlDto.java`
- Create: `src/main/java/com/sosehl/curtis/domain/v1/quiz/dto/QuestionYamlDto.java`

These are structural data classes (validation annotations, no independent behavior); they're exercised by `QuizYamlImportServiceTest` in Task 7. No dedicated test file.

- [ ] **Step 1: Create `QuestionYamlDto`**

```java
package com.sosehl.curtis.domain.v1.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class QuestionYamlDto {

    @NotBlank(message = "Musíte zadat otázku")
    private String question;

    @NotNull(message = "Čas musí být vyplněn")
    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Integer timeInSeconds;

    @NotNull(message = "Musíte zadat možné odpovědi")
    @Size(min = 2, message = "Otázka musí mít alespoň dvě možnosti")
    private List<String> options;

    @NotEmpty(message = "Musíte označit alespoň jednu správnou odpověď")
    private List<Integer> correctIndexes;
}
```

- [ ] **Step 2: Create `QuizYamlDto`**

```java
package com.sosehl.curtis.domain.v1.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuizYamlDto {

    private UUID uuid;

    @NotBlank(message = "Musíte zadat název kvízu")
    @Size(min = 1, max = 100, message = "Název musí mít 1 až 100 znaků")
    private String title;

    private String description;

    @NotNull(message = "maxQuestionsPerSession je povinný")
    private Integer maxQuestionsPerSession;

    private Boolean shuffle = false;

    @NotEmpty(message = "Kvíz musí mít alespoň jednu otázku")
    @Valid
    private List<QuestionYamlDto> questions;
}
```

- [ ] **Step 3: Compile to verify no errors**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/sosehl/curtis/domain/v1/quiz/dto/QuizYamlDto.java src/main/java/com/sosehl/curtis/domain/v1/quiz/dto/QuestionYamlDto.java
git commit -m "feat(quiz): add YAML import DTOs"
```

---

### Task 6: Project setup for YAML import (dependency, scheduling, config)

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/java/com/sosehl/curtis/CurtisBackendApplication.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/test/resources/application-test.properties`

- [ ] **Step 1: Add the YAML dependency to `build.gradle`**

In `build.gradle`, in the `dependencies { ... }` block, add this line directly under the `mapstruct` line:

```groovy
    implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml'
```

(No version needed — Spring Boot's dependency management BOM pins the Jackson family, including this module, to a version compatible with the rest of the stack.)

- [ ] **Step 2: Enable scheduling in `CurtisBackendApplication`**

Replace the full contents of `src/main/java/com/sosehl/curtis/CurtisBackendApplication.java` with:

```java
package com.sosehl.curtis;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CurtisBackendApplication {

    public static void main(String[] args) {
        MicrosoftOauthProperties();

        SpringApplication app = new SpringApplication(
            CurtisBackendApplication.class
        );

        app.run(args);
    }

    private static void MicrosoftOauthProperties() {
        Dotenv dotenv = Dotenv.load();
        String clientSecret = dotenv.get("CLIENT_SECRET");
        System.setProperty(
            "spring.security.oauth2.client.registration.microsoft.client-secret",
            clientSecret
        );

        System.out.println(
            System.getProperty(
                "spring.security.oauth2.client.registration.microsoft.client-secret"
            )
        );
    }
}
```

- [ ] **Step 3: Add import config to `application.properties`**

Append to the end of `src/main/resources/application.properties`:

```properties

# =========================
# YAML import (drop-folder)
# =========================
quiz.import.enabled=true
quiz.import.path=./import
```

- [ ] **Step 4: Add import config to `application-test.properties`**

Append to the end of `src/test/resources/application-test.properties`:

```properties

# =========================
# YAML import (drop-folder) - disabled by default in tests
# =========================
quiz.import.enabled=false
```

(Individual tests that need it active override `quiz.import.enabled`/`quiz.import.path` via `@DynamicPropertySource` — see Task 7. All other existing test classes are unaffected and won't create any `import/` directories on disk.)

- [ ] **Step 5: Verify the project still builds**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add build.gradle src/main/java/com/sosehl/curtis/CurtisBackendApplication.java src/main/resources/application.properties src/test/resources/application-test.properties
git commit -m "chore(quiz): add yaml dependency, scheduling, and import config"
```

---

### Task 7: YAML drop-folder import service

**Files:**
- Create: `src/main/java/com/sosehl/curtis/domain/v1/quiz/QuizYamlImportService.java`
- Test: `src/test/java/com/sosehl/curtis/quiz/QuizYamlImportServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.sosehl.curtis.quiz;

import static org.assertj.core.api.Assertions.*;

import com.sosehl.curtis.domain.v1.quiz.Quiz;
import com.sosehl.curtis.domain.v1.quiz.QuizRepository;
import com.sosehl.curtis.domain.v1.quiz.QuizYamlImportService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class QuizYamlImportServiceTest {

    @TempDir
    static Path importDir;

    @DynamicPropertySource
    static void importProperties(DynamicPropertyRegistry registry) {
        registry.add("quiz.import.enabled", () -> "true");
        registry.add("quiz.import.path", importDir::toString);
    }

    @Autowired
    private QuizYamlImportService importService;

    @Autowired
    private QuizRepository quizRepository;

    private Path writeYaml(String filename, String content)
        throws IOException {
        Path file = importDir.resolve(filename);
        Files.writeString(file, content);
        return file;
    }

    private long countFilesIn(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.count();
        }
    }

    @Test
    void shouldImportValidQuizAndMoveToProcessed() throws IOException {
        Path file = writeYaml(
            "valid.yaml",
            """
            title: "Chapter 5 Quiz"
            description: "Cell biology basics"
            shuffle: true
            maxQuestionsPerSession: 10
            questions:
              - question: "What is the powerhouse of the cell?"
                timeInSeconds: 20
                options: ["Mitochondria", "Nucleus", "Ribosome"]
                correctIndexes: [0]
            """
        );

        importService.processFile(file);

        assertThat(Files.exists(file)).isFalse();
        assertThat(countFilesIn(importDir.resolve("processed"))).isEqualTo(1);

        Optional<Quiz> saved = quizRepository
            .findAll()
            .stream()
            .filter(q -> "Chapter 5 Quiz".equals(q.getTitle()))
            .findFirst();
        assertThat(saved).isPresent();
        assertThat(saved.get().getQuestions()).hasSize(1);
        assertThat(saved.get().getQuestions().get(0).getAnswers()).hasSize(3);
    }

    @Test
    void shouldMoveMalformedYamlToFailedWithErrorFile() throws IOException {
        Path file = writeYaml(
            "broken.yaml",
            "title: [this is not a valid quiz structure\n"
        );

        importService.processFile(file);

        assertThat(Files.exists(file)).isFalse();
        assertThat(countFilesIn(importDir.resolve("failed"))).isEqualTo(2);
    }

    @Test
    void shouldFailValidationWhenNoCorrectAnswerMarked() throws IOException {
        Path file = writeYaml(
            "no-correct.yaml",
            """
            title: "Bad Quiz"
            maxQuestionsPerSession: 5
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                options: ["A", "B"]
                correctIndexes: []
            """
        );

        importService.processFile(file);

        assertThat(Files.exists(file)).isFalse();
        assertThat(countFilesIn(importDir.resolve("failed"))).isEqualTo(2);
    }

    @Test
    void shouldFailWhenCorrectIndexOutOfRange() throws IOException {
        Path file = writeYaml(
            "bad-index.yaml",
            """
            title: "Bad Index Quiz"
            maxQuestionsPerSession: 5
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                options: ["A", "B"]
                correctIndexes: [5]
            """
        );

        importService.processFile(file);

        assertThat(countFilesIn(importDir.resolve("failed"))).isEqualTo(2);
    }

    @Test
    void shouldFailWhenReplaceTargetUuidDoesNotExist() throws IOException {
        Path file = writeYaml(
            "unknown-uuid.yaml",
            """
            uuid: "%s"
            title: "Replacement Quiz"
            maxQuestionsPerSession: 5
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                options: ["A", "B"]
                correctIndexes: [0]
            """.formatted(UUID.randomUUID())
        );

        importService.processFile(file);

        assertThat(countFilesIn(importDir.resolve("failed"))).isEqualTo(2);
    }

    @Test
    void shouldReplaceExistingQuizWhenUuidMatches() throws IOException {
        Quiz existing = new Quiz();
        existing.setTitle("Old Title");
        existing.setMaxQuestionsPerSession(5);
        existing.setShuffle(false);
        UUID uuid = quizRepository.save(existing).getUuid();

        Path file = writeYaml(
            "replace.yaml",
            """
            uuid: "%s"
            title: "New Title"
            maxQuestionsPerSession: 3
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                options: ["A", "B"]
                correctIndexes: [0]
            """.formatted(uuid)
        );

        importService.processFile(file);

        Quiz updated = quizRepository.findByUuid(uuid).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getMaxQuestionsPerSession()).isEqualTo(3);
        assertThat(updated.getQuestions()).hasSize(1);
        assertThat(countFilesIn(importDir.resolve("processed"))).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.sosehl.curtis.quiz.QuizYamlImportServiceTest"`
Expected: FAIL — compilation error, `QuizYamlImportService` does not exist.

- [ ] **Step 3: Implement `QuizYamlImportService`**

```java
package com.sosehl.curtis.domain.v1.quiz;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.sosehl.curtis.domain.v1.question.Question;
import com.sosehl.curtis.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis.domain.v1.quiz.dto.QuestionYamlDto;
import com.sosehl.curtis.domain.v1.quiz.dto.QuizYamlDto;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class QuizYamlImportService {

    private static final Logger log = LoggerFactory.getLogger(
        QuizYamlImportService.class
    );
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final QuizRepository quizRepository;
    private final Validator validator;
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @Value("${quiz.import.enabled:true}")
    private boolean importEnabled;

    @Value("${quiz.import.path:./import}")
    private String importPathProperty;

    private Path importPath;
    private Path processedPath;
    private Path failedPath;

    public QuizYamlImportService(
        QuizRepository quizRepository,
        Validator validator
    ) {
        this.quizRepository = quizRepository;
        this.validator = validator;
    }

    @PostConstruct
    void init() throws IOException {
        if (!importEnabled) {
            return;
        }

        importPath = Paths.get(importPathProperty);
        processedPath = importPath.resolve("processed");
        failedPath = importPath.resolve("failed");
        Files.createDirectories(processedPath);
        Files.createDirectories(failedPath);
    }

    @Scheduled(fixedDelay = 5000)
    public void pollImportFolder() {
        if (!importEnabled) {
            return;
        }

        try (Stream<Path> files = Files.list(importPath)) {
            files
                .filter(Files::isRegularFile)
                .filter(this::isYamlFile)
                .forEach(this::processFile);
        } catch (IOException e) {
            log.error(
                "Nepodařilo se přečíst import složku {}",
                importPath,
                e
            );
        }
    }

    public void processFile(Path file) {
        QuizYamlDto dto;
        try {
            dto = yamlMapper.readValue(file.toFile(), QuizYamlDto.class);
            validate(dto);
        } catch (Exception e) {
            moveToFailed(file, describe(e));
            return;
        }

        UUID quizUuid;
        try {
            quizUuid = importQuiz(dto);
        } catch (Exception e) {
            moveToFailed(file, describe(e));
            return;
        }

        moveToProcessed(file, quizUuid);
    }

    private boolean isYamlFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private void validate(QuizYamlDto dto) {
        Set<ConstraintViolation<QuizYamlDto>> violations = validator.validate(
            dto
        );
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                violations
                    .stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "))
            );
        }

        List<QuestionYamlDto> questions = dto.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            QuestionYamlDto q = questions.get(i);
            for (Integer idx : q.getCorrectIndexes()) {
                if (idx == null || idx < 0 || idx >= q.getOptions().size()) {
                    throw new IllegalArgumentException(
                        "Otázka " +
                        (i + 1) +
                        ": correctIndexes obsahuje neplatný index " +
                        idx
                    );
                }
            }
        }
    }

    private UUID importQuiz(QuizYamlDto dto) {
        Quiz quiz;
        if (dto.getUuid() != null) {
            quiz = quizRepository
                .findByUuid(dto.getUuid())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Kvíz s UUID " + dto.getUuid() + " neexistuje"
                    )
                );
        } else {
            quiz = new Quiz();
            quiz.setCreatedAt(LocalDateTime.now());
        }

        quiz.setTitle(dto.getTitle());
        quiz.setDescription(dto.getDescription());
        quiz.setMaxQuestionsPerSession(dto.getMaxQuestionsPerSession());
        quiz.setShuffle(dto.getShuffle());
        quiz.setEditedAt(LocalDateTime.now());
        quiz.setQuestions(
            dto.getQuestions().stream().map(q -> toQuestion(q, quiz)).toList()
        );

        return quizRepository.save(quiz).getUuid();
    }

    private Question toQuestion(QuestionYamlDto dto, Quiz quiz) {
        Question question = new Question();
        question.setQuestion(dto.getQuestion());
        question.setTimeInSeconds(dto.getTimeInSeconds());
        question.setQuiz(quiz);

        List<QuestionAnswer> answers = new ArrayList<>();
        for (int i = 0; i < dto.getOptions().size(); i++) {
            QuestionAnswer answer = new QuestionAnswer();
            answer.setAnswer(dto.getOptions().get(i));
            answer.setIsCorrect(dto.getCorrectIndexes().contains(i));
            answers.add(answer);
        }
        question.setAnswers(answers);
        return question;
    }

    private void moveToProcessed(Path file, UUID quizUuid) {
        try {
            Path target = processedPath.resolve(timestampedName(file));
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            log.info(
                "Import {} proběhl úspěšně, quizUuid={}",
                file.getFileName(),
                quizUuid
            );
        } catch (IOException e) {
            log.error(
                "Import {} proběhl úspěšně (quizUuid={}), ale soubor se nepodařilo přesunout",
                file.getFileName(),
                quizUuid,
                e
            );
        }
    }

    private void moveToFailed(Path file, String reason) {
        try {
            String name = timestampedName(file);
            Files.move(
                file,
                failedPath.resolve(name),
                StandardCopyOption.REPLACE_EXISTING
            );
            Files.writeString(failedPath.resolve(name + ".error.txt"), reason);
            log.warn("Import {} selhal: {}", file.getFileName(), reason);
        } catch (IOException e) {
            log.error(
                "Nepodařilo se přesunout selhaný soubor {}",
                file.getFileName(),
                e
            );
        }
    }

    private String timestampedName(Path file) {
        return (
            TIMESTAMP_FORMAT.format(LocalDateTime.now()) +
            "-" +
            file.getFileName()
        );
    }

    private String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.sosehl.curtis.quiz.QuizYamlImportServiceTest"`
Expected: PASS (6 tests)

- [ ] **Step 5: Run the full test suite**

Run: `./gradlew test`
Expected: PASS — all test classes across the project succeed.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sosehl/curtis/domain/v1/quiz/QuizYamlImportService.java src/test/java/com/sosehl/curtis/quiz/QuizYamlImportServiceTest.java
git commit -m "feat(quiz): add scheduled drop-folder YAML quiz importer"
```

---

### Task 8: Final verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew test`
Expected: PASS — every test class in the project (existing + new) passes.

- [ ] **Step 2: Run a full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Confirm no `import/` directory was left in the repo root by test runs**

Run: `git status`
Expected: clean — no untracked `import/` directory. If one appears, it means a test class picked up `quiz.import.enabled=true` unexpectedly; check that only `QuizYamlImportServiceTest` overrides it via `@DynamicPropertySource` and that `application-test.properties` still sets `quiz.import.enabled=false`.

- [ ] **Step 4: Manually sanity-check the drop-folder flow** (optional, requires a running Postgres per `application.properties`)

```bash
mkdir -p import
cat > import/manual-test.yaml <<'EOF'
title: "Manual Smoke Test"
maxQuestionsPerSession: 1
questions:
  - question: "2 + 2 = ?"
    timeInSeconds: 15
    options: ["3", "4"]
    correctIndexes: [1]
EOF
./gradlew bootRun
```

Within ~5 seconds of startup, `import/manual-test.yaml` should disappear and a corresponding file should appear under `import/processed/`. Stop the app with `Ctrl+C` afterward.

---

## Notes for the executing engineer

- Every `@WithMockUser`/`SecurityMockMvcRequestPostProcessors.user(...)` call for an endpoint that creates/patches a quiz, creates/patches/deletes a question, or creates a session now needs `ROLE_TEACHER` — if you add new tests for those endpoints later, remember this.
- The grace period (`GRACE_PERIOD_SECONDS = 2`) in `StudentAttempt` is a fixed constant, not configurable — this matches the spec's YAGNI call, don't add a config property for it unless a real need shows up.
- The YAML importer never touches the web layer — there is intentionally no REST endpoint, no controller, no auth check for it. Don't be tempted to add one; that was explicitly ruled out in the spec because the teacher hosts the server themselves.
- `Quiz.questions` relies on `orphanRemoval = true` + `cascade = CascadeType.ALL` (already present on the entity) for the replace-by-uuid path to correctly delete old questions when `quiz.setQuestions(newList)` is called — don't add manual delete calls, they're redundant and risk double-delete errors.
