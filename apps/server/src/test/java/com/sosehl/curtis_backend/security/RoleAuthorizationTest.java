package com.sosehl.curtis_backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionCreateDto;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
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
    void shouldForbidSessionResultsWithoutTeacherRole() throws Exception {
        mockMvc
            .perform(
                get("/v1/sessions/" + existingQuizUuid + "/results")
                    .with(SecurityMockMvcRequestPostProcessors.user("student1"))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidPendingTextAnswersWithoutTeacherRole() throws Exception {
        mockMvc
            .perform(
                get("/v1/sessions/" + existingQuizUuid + "/pending-text-answers")
                    .with(SecurityMockMvcRequestPostProcessors.user("student1"))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidYamlImportWithoutTeacherRole() throws Exception {
        mockMvc
            .perform(
                multipart("/v1/quiz/import")
                    .file(
                        new org.springframework.mock.web.MockMultipartFile(
                            "file",
                            "quiz.yaml",
                            "application/yaml",
                            "title: Quiz\nmaxQuestionsPerSession: 1\nquestions: []\n".getBytes()
                        )
                    )
                    .with(SecurityMockMvcRequestPostProcessors.user("student1"))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowQuizReadsWithoutTeacherRole() throws Exception {
        // Students can now view quizzes (for session joining)
        mockMvc
            .perform(
                get("/v1/quiz").with(
                    SecurityMockMvcRequestPostProcessors.user("student1")
                )
            )
            .andExpect(status().isOk());
        mockMvc
            .perform(
                get("/v1/quiz/" + existingQuizUuid).with(
                    SecurityMockMvcRequestPostProcessors.user("student1")
                )
            )
            .andExpect(status().isOk());
        mockMvc
            .perform(
                get("/v1/quiz/" + existingQuizUuid + "/questions").with(
                    SecurityMockMvcRequestPostProcessors.user("student1")
                )
            )
            .andExpect(status().isOk());
    }

    @Test
    void shouldAllowQuizReadsWithTeacherRole() throws Exception {
        mockMvc
            .perform(
                get("/v1/quiz").with(
                    SecurityMockMvcRequestPostProcessors
                        .user("teacher4")
                        .roles("TEACHER")
                )
            )
            .andExpect(status().isOk());
        mockMvc
            .perform(
                get("/v1/quiz/" + existingQuizUuid + "/questions").with(
                    SecurityMockMvcRequestPostProcessors
                        .user("teacher4")
                        .roles("TEACHER")
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldAllowSessionResultsWithTeacherRole() throws Exception {
        mockMvc
            .perform(
                get("/v1/sessions/" + existingQuizUuid + "/results")
                    .with(
                        SecurityMockMvcRequestPostProcessors
                            .user("teacher3")
                            .roles("TEACHER")
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
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
