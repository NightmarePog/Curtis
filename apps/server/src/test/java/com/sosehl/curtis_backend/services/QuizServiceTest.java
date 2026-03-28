package com.sosehl.curtis_backend.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.mappers.QuizMapper;
import com.sosehl.curtis_backend.models.QuizModel;
import com.sosehl.curtis_backend.repositories.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class QuizServiceTest {

    @Mock
    private QuizRepository repository;

    @Mock
    private QuizMapper mapper;

    @InjectMocks
    private QuizService quizService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateQuiz() {
        QuizCreateRequest request = new QuizCreateRequest();
        request.setTitle("Test Quiz");
        request.setDescription("Popis quizu");

        QuizModel mappedQuiz = new QuizModel();
        mappedQuiz.setTitle(request.getTitle());
        mappedQuiz.setDescription(request.getDescription());

        when(mapper.mapCreateQuiz(any(QuizCreateRequest.class))).thenReturn(
            mappedQuiz
        );
        when(repository.save(any(QuizModel.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        quizService.createQuiz(request);

        verify(repository, times(1)).save(mappedQuiz);
    }
}
