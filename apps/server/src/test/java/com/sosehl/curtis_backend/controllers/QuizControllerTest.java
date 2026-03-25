package com.sosehl.curtis_backend.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.receive.QuizPatchRequest;
import com.sosehl.curtis_backend.services.QuizService;
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
        request.setOrder(0);

        doNothing().when(quizService).createQuiz(request);

        mockMvc
            .perform(
                post("/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated()); // POST → 201

        verify(quizService, times(1)).createQuiz(request);
    }

    @Test
    void shouldReturnCreatedWhenDescriptionMissing() throws Exception {
        QuizCreateRequest request = new QuizCreateRequest();
        request.setTitle("Test Quiz");
        request.setOrder(0); // description nepovinné

        doNothing().when(quizService).createQuiz(request);

        mockMvc
            .perform(
                post("/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated()); // POST → 201

        verify(quizService, times(1)).createQuiz(request);
    }

    @Test
    void shouldPatchQuiz() throws Exception {
        UUID quizUuid = UUID.randomUUID();

        QuizPatchRequest request = new QuizPatchRequest();
        request.setUuid(quizUuid);
        request.setTitle("Nový název");
        request.setDescription("Nový popis");

        doNothing().when(quizService).patchQuiz(request);

        mockMvc
            .perform(
                patch("/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk()); // PATCH → 200

        verify(quizService, times(1)).patchQuiz(request);
    }
}
