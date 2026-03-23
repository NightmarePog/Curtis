package com.sosehl.curtis_backend.controllers;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.services.QuizService;
import com.sosehl.curtis_backend.util.QuizDataGenerator;
import com.sosehl.curtis_backend.util.QuizMvcHelper;
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

    @Test
    void shouldReturnCreated() throws Exception {
        QuizCreateRequest quiz = new QuizDataGenerator()
            .setQuestionCount(5)
            .build();

        QuizMvcHelper.postQuiz(mockMvc, objectMapper, quiz).andExpect(
            status().isCreated()
        );
    }

    @Test
    void shouldReturnBadRequestWhenTitleMissing() throws Exception {
        QuizCreateRequest quiz = new QuizDataGenerator()
            .setQuestionCount(3)
            .build();
        quiz.setTitle(null);

        QuizMvcHelper.postQuiz(mockMvc, objectMapper, quiz).andExpect(
            status().isBadRequest()
        );
    }

    @Test
    void shouldReturnBadRequestWhenDescriptionMissing() throws Exception {
        QuizCreateRequest quiz = new QuizDataGenerator()
            .setQuestionCount(3)
            .build();
        quiz.setDescription(null);

        QuizMvcHelper.postQuiz(mockMvc, objectMapper, quiz).andExpect(
            status().isBadRequest()
        );
    }
}
