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
    public void create(QuestionCreateDto qDto, UUID quizUuid) {
        Quiz quiz = quizRepository
            .findByUuid(quizUuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kvíz nebyl nalezen"
                )
            );

        Question question = mapper.createMap(qDto, quiz);
        repository.save(question);
    }

    @Transactional
    public void patch(QuestionPatchDto qDto, UUID quizUuid) {
        List<Question> questions = repository
            .findByQuiz_Uuid(quizUuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kvíz nebyl nalezen"
                )
            );

        Question question = questions
            .stream()
            .filter(q -> q.getId().equals(qDto.getQuestionId()))
            .findFirst()
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Otázka nebyla nalezena"
                )
            );

        mapper.patchMap(qDto, question);
        repository.saveAll(questions);
    }

    @Transactional
    public void delete(UUID quizUuid, Integer questionId) {
        List<Question> questions = repository
            .findByQuiz_Uuid(quizUuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kvíz nebyl nalezen"
                )
            );

        Question question = questions
            .stream()
            .filter(q -> q.getId().equals(questionId))
            .findFirst()
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Otázka nebyla nalezena"
                )
            );

        repository.delete(question);
        repository.saveAll(questions);
    }

    public Question get(UUID quizUuid, Integer questionId) {
        return repository
            .findByQuiz_Uuid(quizUuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kvíz nebyl nalezen"
                )
            )
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

    public List<Question> getAll(UUID quizUuid) {
        return repository
            .findByQuiz_Uuid(quizUuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kvíz nebyl nalezen"
                )
            )
            .stream()
            // .sorted(Comparator.comparing(Question::getQuestionOrder))
            .toList();
    }
}
