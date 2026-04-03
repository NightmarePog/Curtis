package com.sosehl.curtis_backend.domain.v1.question;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionCreateDto;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionPatchDto;
import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuestionService {

    private final QuestionRepository repository;
    private final QuizRepository quizRepository;
    private final QuestionMapper mapper;

    public QuestionService(
        QuestionRepository repository,
        QuizRepository quizRepository,
        QuestionMapper mapper
    ) {
        this.repository = repository;
        this.quizRepository = quizRepository;
        this.mapper = mapper;
    }

    @Transactional
    public void create(QuestionCreateDto dto, UUID quizUuid) {
        Quiz quiz = quizRepository
            .findByUuid(quizUuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kvíz nebyl nalezen"
                )
            );

        repository.save(mapper.toEntity(dto, quiz));
    }

    @Transactional
    public void patch(QuestionPatchDto dto, UUID quizUuid) {
        Question question = findQuestion(quizUuid, dto.getQuestionId());
        mapper.updateFromPatch(dto, question);
        repository.save(question);
    }

    @Transactional
    public void delete(UUID quizUuid, Long questionId) {
        repository.delete(findQuestion(quizUuid, questionId));
    }

    public Question get(UUID quizUuid, Long questionId) {
        return findQuestion(quizUuid, questionId);
    }

    public List<Question> getAll(UUID quizUuid) {
        return getQuestions(quizUuid);
    }

    private Question findQuestion(UUID quizUuid, Long questionId) {
        return getQuestions(quizUuid)
            .stream()
            .filter(q -> q.getId().equals(questionId))
            .findFirst()
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Otázka nebyla nalezena"
                )
            );
    }

    private List<Question> getQuestions(UUID quizUuid) {
        return repository
            .findByQuiz_Uuid(quizUuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kvíz nebyl nalezen"
                )
            );
    }
}
