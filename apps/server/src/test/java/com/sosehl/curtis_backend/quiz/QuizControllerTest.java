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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class QuizControllerTest {

    @TempDir
    static Path mediaDir;

    @DynamicPropertySource
    static void mediaProperties(DynamicPropertyRegistry registry) {
        registry.add("quiz.media.path", mediaDir::toString);
    }

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
                    .with(
                        SecurityMockMvcRequestPostProcessors
                            .user("testuser")
                            .roles("TEACHER")
                    )
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
    @WithMockUser(authorities = "ROLE_TEACHER")
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
    @WithMockUser(authorities = "ROLE_TEACHER")
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
    @WithMockUser(authorities = "ROLE_TEACHER")
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
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEACHER")
    void shouldGetQuizByUuid() throws Exception {
        mockMvc
            .perform(get("/v1/quiz/" + createdQuizUuid))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test kvíz"))
            .andExpect(jsonPath("$.description").value("Popis"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEACHER")
    void shouldReturnNotFoundForUnknownUuid() throws Exception {
        mockMvc
            .perform(get("/v1/quiz/" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEACHER")
    void shouldReturnAllQuizzes() throws Exception {
        mockMvc
            .perform(get("/v1/quiz"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldAllowPublicQuizListing() throws Exception {
        // Quiz listing is now public so students can browse available quizzes
        mockMvc.perform(get("/v1/quiz")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEACHER")
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
    @WithMockUser(authorities = "ROLE_TEACHER")
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
    @WithMockUser(authorities = "ROLE_TEACHER")
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
    void shouldReturnUnauthorizedPatchWhenNotAuthenticated() throws Exception {
        QuizPatchRequest patch = new QuizPatchRequest();
        patch.setTitle("Nový název");

        mockMvc
            .perform(
                patch("/v1/quiz/" + createdQuizUuid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(patch))
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEACHER")
    void shouldImportYamlWithImageAttachment() throws Exception {
        MockMultipartFile yaml = new MockMultipartFile(
            "file",
            "quiz.yaml",
            "application/yaml",
            """
            title: "Uploaded quiz"
            maxQuestionsPerSession: 5
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                imageRef: "diagram.png"
                options: ["A", "B"]
                correctIndexes: [0]
            """.getBytes()
        );
        MockMultipartFile image = new MockMultipartFile(
            "images",
            "diagram.png",
            "image/png",
            new byte[] { 1, 2, 3 }
        );

        mockMvc
            .perform(multipart("/v1/quiz/import").file(yaml).file(image))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quizUuid").isNotEmpty());

        org.assertj.core.api.Assertions.assertThat(
            java.nio.file.Files.readAllBytes(mediaDir.resolve("diagram.png"))
        ).containsExactly(1, 2, 3);
    }

    @Test
    @WithMockUser(authorities = "ROLE_TEACHER")
    void shouldRejectUnsafeImageAttachmentName() throws Exception {
        MockMultipartFile yaml = new MockMultipartFile(
            "file",
            "quiz.yaml",
            "application/yaml",
            """
            title: "Uploaded quiz"
            maxQuestionsPerSession: 5
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                imageRef: "diagram.png"
                options: ["A", "B"]
                correctIndexes: [0]
            """.getBytes()
        );
        MockMultipartFile image = new MockMultipartFile(
            "images",
            "../secret.png",
            "image/png",
            new byte[] { 1 }
        );

        mockMvc
            .perform(multipart("/v1/quiz/import").file(yaml).file(image))
            .andExpect(status().isBadRequest());
    }
}
