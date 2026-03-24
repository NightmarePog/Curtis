package com.sosehl.curtis_backend.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.receive.QuizPatchRequest;
import com.sosehl.curtis_backend.dto.response.QuizGetResponse;
import com.sosehl.curtis_backend.services.QuizService;
import com.sosehl.curtis_backend.util.QuizDataGenerator;
import com.sosehl.curtis_backend.util.QuizMvcHelper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuizService quizService;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------------- POST ----------------

    @Test
    void shouldReturnCreated() throws Exception {
        QuizCreateRequest quiz = new QuizDataGenerator().build();

        mockMvc
            .perform(QuizMvcHelper.postQuiz(objectMapper, quiz))
            .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnBadRequestWhenTitleMissing() throws Exception {
        QuizCreateRequest quiz = new QuizDataGenerator().build();
        quiz.setTitle(null);

        mockMvc
            .perform(QuizMvcHelper.postQuiz(objectMapper, quiz))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnCreatedWhenDescriptionMissing() throws Exception {
        QuizCreateRequest quiz = new QuizDataGenerator().build();
        quiz.setDescription(null);

        mockMvc
            .perform(QuizMvcHelper.postQuiz(objectMapper, quiz))
            .andExpect(status().isCreated());
    }

    // ---------------- GET ALL ----------------

    @Test
    void shouldReturnAllQuizzes() throws Exception {
        when(quizService.returnAllQuizzes()).thenReturn(
            Optional.of(List.of(new QuizGetResponse()))
        );

        mockMvc
            .perform(QuizMvcHelper.getAllQuizzes())
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenNoQuizzes() throws Exception {
        when(quizService.returnAllQuizzes()).thenReturn(Optional.empty());

        mockMvc
            .perform(QuizMvcHelper.getAllQuizzes())
            .andExpect(status().isNotFound());
    }

    // ---------------- GET BY UUID ----------------

    @Test
    void shouldReturnQuizByUuid() throws Exception {
        UUID uuid = UUID.randomUUID();

        when(quizService.returnQuiz(uuid)).thenReturn(
            Optional.of(new QuizGetResponse())
        );

        mockMvc.perform(QuizMvcHelper.getQuiz(uuid)).andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundWhenQuizDoesNotExist() throws Exception {
        UUID uuid = UUID.randomUUID();

        when(quizService.returnQuiz(uuid)).thenReturn(Optional.empty());

        mockMvc
            .perform(QuizMvcHelper.getQuiz(uuid))
            .andExpect(status().isNotFound());
    }

    // ---------------- PATCH ----------------

    @Test
    void shouldPatchQuiz() throws Exception {
        QuizPatchRequest patch = new QuizPatchRequest();
        patch.setUuid(UUID.randomUUID());

        mockMvc
            .perform(QuizMvcHelper.patchQuiz(objectMapper, patch))
            .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnBadRequestWhenPatchInvalid() throws Exception {
        QuizPatchRequest patch = new QuizPatchRequest(); // missing UUID

        mockMvc
            .perform(QuizMvcHelper.patchQuiz(objectMapper, patch))
            .andExpect(status().isBadRequest());
    }
}
