package com.sosehl.curtis_backend.quiz;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID createdQuizUuid;

    private QuizCreateRequest validRequest() {
        QuizCreateRequest request = new QuizCreateRequest();
        request.setTitle("Test kvíz");
        request.setDescription("Popis");
        request.setShuffle(false);
        request.setMaxQuestionsPerSession(10);
        return request;
    }

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc
            .perform(
                post("/v1/quiz")
                    .with(SecurityMockMvcRequestPostProcessors.user("testuser"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest()))
            )
            .andExpect(status().isCreated())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        createdQuizUuid = objectMapper
            .readTree(body)
            .get("quizUuid")
            .traverse(objectMapper)
            .readValueAs(UUID.class);
    }

    @Test
    @WithMockUser
    void shouldCreateQuiz() throws Exception {
        QuizCreateRequest request = validRequest();
        request.setTitle("Nový kvíz");

        mockMvc
            .perform(
                post("/v1/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quizUuid").isNotEmpty());
    }

    @Test
    @WithMockUser
    void shouldFailCreateWhenTitleIsMissing() throws Exception {
        QuizCreateRequest request = validRequest();
        request.setTitle(null);

        mockMvc
            .perform(
                post("/v1/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldFailCreateWhenTitleIsTooLong() throws Exception {
        QuizCreateRequest request = validRequest();
        request.setTitle("a".repeat(101));

        mockMvc
            .perform(
                post("/v1/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailCreateWhenNotAuthenticated() throws Exception {
        QuizCreateRequest request = validRequest();

        mockMvc
            .perform(
                post("/v1/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isFound());
    }

    @Test
    @WithMockUser
    void shouldGetQuizByUuid() throws Exception {
        mockMvc
            .perform(get("/v1/quiz/" + createdQuizUuid))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test kvíz"))
            .andExpect(jsonPath("$.description").value("Popis"));
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundForUnknownUuid() throws Exception {
        mockMvc
            .perform(get("/v1/quiz/" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldReturnAllQuizzes() throws Exception {
        mockMvc
            .perform(get("/v1/quiz"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldRedirectGetWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/v1/quiz")).andExpect(status().isFound());
    }

    @Test
    @WithMockUser
    void shouldPatchQuizTitle() throws Exception {
        QuizPatchRequest patch = new QuizPatchRequest();
        patch.setTitle("Upravený název");

        mockMvc
            .perform(
                patch("/v1/quiz/" + createdQuizUuid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patch))
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(get("/v1/quiz/" + createdQuizUuid))
            .andExpect(jsonPath("$.title").value("Upravený název"));
    }

    @Test
    @WithMockUser
    void shouldPatchOnlyProvidedFields() throws Exception {
        QuizPatchRequest patch = new QuizPatchRequest();
        patch.setDescription("Nový popis");

        mockMvc
            .perform(
                patch("/v1/quiz/" + createdQuizUuid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patch))
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(get("/v1/quiz/" + createdQuizUuid))
            .andExpect(jsonPath("$.title").value("Test kvíz"))
            .andExpect(jsonPath("$.description").value("Nový popis"));
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundWhenPatchingUnknownQuiz() throws Exception {
        QuizPatchRequest patch = new QuizPatchRequest();
        patch.setTitle("Nový název");

        mockMvc
            .perform(
                patch("/v1/quiz/" + UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patch))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldRedirectPatchWhenNotAuthenticated() throws Exception {
        QuizPatchRequest patch = new QuizPatchRequest();
        patch.setTitle("Nový název");

        mockMvc
            .perform(
                patch("/v1/quiz/" + createdQuizUuid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patch))
            )
            .andExpect(status().isFound());
    }
}
