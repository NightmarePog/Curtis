package com.sosehl.curtis_backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sosehl.curtis_backend.dto.receive.QuestionCreateDto;
import com.sosehl.curtis_backend.mappers.QuestionMapper;
import com.sosehl.curtis_backend.models.QuestionModel;
import com.sosehl.curtis_backend.models.QuizModel;
import com.sosehl.curtis_backend.repositories.QuestionRepository;
import com.sosehl.curtis_backend.repositories.QuizRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private QuestionService questionService;

    private UUID quizUuid;
    private QuizModel quiz;
    private QuestionModel question;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        quizUuid = UUID.randomUUID();
        quiz = new QuizModel();
        quiz.setUuid(quizUuid);

        question = new QuestionModel();
        question.setQuestionOrder(1);
        question.setQuiz(quiz);
    }

    @Test
    void testCreate() {
        QuestionCreateDto dto = new QuestionCreateDto();
        dto.setQuestionOrder(1);
        dto.setQuestion("Test question");

        when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.of(quiz));
        when(questionMapper.createMap(dto, quiz)).thenReturn(question);

        questionService.create(dto, quizUuid);

        verify(questionRepository, times(1)).save(question);
    }

    @Test
    void testGetAll() {
        when(questionRepository.findByQuiz_Uuid(quizUuid)).thenReturn(
            Optional.of(List.of(question))
        );

        List<QuestionModel> result = questionService.getAll(quizUuid);

        assertEquals(1, result.size());
        assertEquals(question, result.get(0));
    }
}
