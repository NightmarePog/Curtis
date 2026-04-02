package com.sosehl.curtis_backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizController;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizService;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class QuizControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController quizController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(quizController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldReturnCreated() throws Exception {
        QuizCreateRequest request = new QuizCreateRequest();
        request.setTitle("Test Quiz");
        request.setDescription("Popis quizu");

        mockMvc
            .perform(
                post("/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated());

        verify(quizService, times(1)).createQuiz(any(QuizCreateRequest.class));
    }

    @Test
    void shouldReturnCreatedWhenDescriptionMissing() throws Exception {
        QuizCreateRequest request = new QuizCreateRequest();
        request.setTitle("Test Quiz");

        mockMvc
            .perform(
                post("/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated());

        verify(quizService, times(1)).createQuiz(any(QuizCreateRequest.class));
    }

    @Test
    void shouldPatchQuiz() throws Exception {
        UUID quizUuid = UUID.randomUUID();

        QuizPatchRequest request = new QuizPatchRequest();
        request.setUuid(quizUuid);
        request.setTitle("Nový název");
        request.setDescription("Nový popis");

        mockMvc
            .perform(
                patch("/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk());

        verify(quizService, times(1)).patchQuiz(any(QuizPatchRequest.class));
    }
}
