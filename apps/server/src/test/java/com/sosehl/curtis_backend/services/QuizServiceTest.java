package com.sosehl.curtis_backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.receive.QuizPatchRequest;
import com.sosehl.curtis_backend.dto.response.QuizGetResponse;
import com.sosehl.curtis_backend.mappers.QuizMapper;
import com.sosehl.curtis_backend.models.QuizModel;
import com.sosehl.curtis_backend.repositories.QuizRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizMapper quizMapper;

    @InjectMocks
    private QuizService quizService;

    private QuizCreateRequest createRequest;
    private QuizPatchRequest patchRequest;
    private QuizModel quizModel;
    private QuizGetResponse quizResponse;
    private UUID quizUuid;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        quizUuid = UUID.randomUUID();

        createRequest = new QuizCreateRequest();
        createRequest.setTitle("Test Quiz");
        createRequest.setDescription("Test Description");

        patchRequest = new QuizPatchRequest();
        patchRequest.setUuid(quizUuid);
        patchRequest.setTitle("Updated Title");
        patchRequest.setDescription("Updated Description");

        quizModel = new QuizModel();
        quizModel.setUuid(quizUuid);

        quizResponse = new QuizGetResponse();
        quizResponse.setUuid(quizUuid);
        quizResponse.setTitle("Test Quiz");
        quizResponse.setDescription("Test Description");
    }

    @Test
    void testCreateQuiz() {
        when(quizMapper.mapCreateQuiz(createRequest)).thenReturn(quizModel);

        quizService.createQuiz(createRequest);

        verify(quizRepository, times(1)).save(quizModel);
    }

    @Test
    void testReturnQuizFound() {
        when(quizRepository.findByUuid(quizUuid)).thenReturn(
            Optional.of(quizModel)
        );
        when(quizMapper.mapGetResponse(quizModel)).thenReturn(quizResponse);

        Optional<QuizGetResponse> result = quizService.returnQuiz(quizUuid);

        assertTrue(result.isPresent());
        assertEquals(quizUuid, result.get().getUuid());
        assertEquals("Test Quiz", result.get().getTitle());
    }

    @Test
    void testReturnQuizNotFound() {
        when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.empty());

        Optional<QuizGetResponse> result = quizService.returnQuiz(quizUuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testReturnAllQuizzes() {
        when(quizRepository.findAll()).thenReturn(List.of(quizModel));
        when(quizMapper.mapGetResponse(quizModel)).thenReturn(quizResponse);

        Optional<List<QuizGetResponse>> result = quizService.returnAllQuizzes();

        assertTrue(result.isPresent());
        assertEquals(1, result.get().size());
        assertEquals(quizUuid, result.get().get(0).getUuid());
    }

    @Test
    void testPatchQuizSuccess() {
        when(quizRepository.findByUuid(quizUuid)).thenReturn(
            Optional.of(quizModel)
        );
        when(quizMapper.mapPatchQuiz(patchRequest, quizModel)).thenReturn(
            quizModel
        );

        quizService.patchQuiz(patchRequest);

        verify(quizRepository, times(1)).save(quizModel);
    }

    @Test
    void testPatchQuizNotFound() {
        when(quizRepository.findByUuid(quizUuid)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> quizService.patchQuiz(patchRequest)
        );

        assertEquals(
            "404 NOT_FOUND \"Quiz not found\"",
            exception.getMessage()
        );
    }
}
