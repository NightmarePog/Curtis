package com.sosehl.curtis_backend.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.dto.receive.QuestionCreateDto;
import com.sosehl.curtis_backend.dto.receive.QuestionPatchDto;
import com.sosehl.curtis_backend.models.QuestionModel;
import com.sosehl.curtis_backend.services.QuestionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class QuestionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionController questionController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(questionController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateQuestion() throws Exception {
        UUID quizUuid = UUID.randomUUID();

        QuestionCreateDto dto = new QuestionCreateDto();
        dto.setQuestion("Otázka?");
        dto.setAnswers(List.of("A", "B"));
        dto.setCorrectAnswers(List.of(0));
        dto.setAnswersId(List.of(1, 2));
        dto.setTime(30);
        dto.setQuestionOrder(0);

        doNothing().when(questionService).create(dto, quizUuid);

        mockMvc
            .perform(
                post("/quiz/{quizUuid}/", quizUuid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isOk());

        verify(questionService, times(1)).create(dto, quizUuid);
    }

    @Test
    void testGetQuestion() throws Exception {
        UUID quizUuid = UUID.randomUUID();
        Integer order = 0;

        QuestionModel question = new QuestionModel();
        question.setQuestionOrder(order);

        when(questionService.get(quizUuid, order)).thenReturn(question);

        mockMvc
            .perform(get("/quiz/{quizUuid}/{questionId}", quizUuid, order))
            .andExpect(status().isOk());

        verify(questionService, times(1)).get(quizUuid, order);
    }

    @Test
    void testGetAllQuestions() throws Exception {
        UUID quizUuid = UUID.randomUUID();

        QuestionModel question1 = new QuestionModel();
        question1.setQuestionOrder(0);

        QuestionModel question2 = new QuestionModel();
        question2.setQuestionOrder(1);

        List<QuestionModel> questions = List.of(question1, question2);

        when(questionService.getAll(quizUuid)).thenReturn(questions);

        mockMvc
            .perform(get("/quiz/{quizUuid}/", quizUuid))
            .andExpect(status().isOk());

        verify(questionService, times(1)).getAll(quizUuid);
    }

    @Test
    void testPatchQuestion() throws Exception {
        UUID quizUuid = UUID.randomUUID();
        Integer questionId = 0;

        QuestionPatchDto dto = new QuestionPatchDto();
        dto.setOriginalOrder(questionId);
        dto.setNewOrder(1);

        doNothing().when(questionService).patch(dto, quizUuid);

        mockMvc
            .perform(
                patch("/quiz/{quizUuid}/{questionId}", quizUuid, questionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isOk());

        verify(questionService, times(1)).patch(dto, quizUuid);
    }

    @Test
    void testDeleteQuestion() throws Exception {
        UUID quizUuid = UUID.randomUUID();
        Integer questionId = 0;

        doNothing().when(questionService).delete(quizUuid, questionId);

        mockMvc
            .perform(
                delete("/quiz/{quizUuid}/{questionId}", quizUuid, questionId)
            )
            .andExpect(status().isOk());

        verify(questionService, times(1)).delete(quizUuid, questionId);
    }
}
