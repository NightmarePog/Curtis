package com.sosehl.curtis_backend.services;

import com.sosehl.curtis_backend.dto.receive.QuestionCreateDto;
import com.sosehl.curtis_backend.dto.receive.QuestionPatchDto;
import com.sosehl.curtis_backend.mappers.QuestionMapper;
import com.sosehl.curtis_backend.models.QuestionModel;
import com.sosehl.curtis_backend.models.QuizModel;
import com.sosehl.curtis_backend.repositories.QuestionRepository;
import com.sosehl.curtis_backend.repositories.QuizRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {

    private final QuestionRepository repository;
    private final QuizRepository quizRepository;
    private final QuestionMapper mapper;

    QuestionService(
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
        QuizModel quizModel = quizRepository
            .findByUuid(quizUuid)
            .orElseThrow(() -> new RuntimeException("Kvíz nebyl nalezen"));

        repository.save(mapper.createMap(qDto, quizModel));
    }

    @Transactional
    public void patch(QuestionPatchDto qDto, UUID quizUuid) {
        List<QuestionModel> questions = repository
            .findByQuiz_Uuid(quizUuid)
            .orElseThrow(() -> new RuntimeException("Kvíz nebyl nalezen"));

        QuestionModel question = questions
            .stream()
            .filter(q -> q.getOrder().equals(qDto.getOriginalOrder()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Otázka nebyla nalezena"));

        if (
            qDto.getNewOrder() != null &&
            !qDto.getNewOrder().equals(qDto.getOriginalOrder())
        ) {
            int oldPos = qDto.getOriginalOrder();
            int newPos = qDto.getNewOrder();

            if (oldPos < newPos) {
                questions
                    .stream()
                    .filter(
                        q -> q.getOrder() > oldPos && q.getOrder() <= newPos
                    )
                    .forEach(q -> q.setOrder(q.getOrder() - 1));
            } else {
                questions
                    .stream()
                    .filter(
                        q -> q.getOrder() >= newPos && q.getOrder() < oldPos
                    )
                    .forEach(q -> q.setOrder(q.getOrder() + 1));
            }
        }

        mapper.patchMap(qDto, question);

        repository.saveAll(questions);
    }

    @Transactional
    public void delete(UUID quizUuid, Integer order) {
        List<QuestionModel> questions = repository
            .findByQuiz_Uuid(quizUuid)
            .orElseThrow(() -> new RuntimeException("Kvíz nebyl nalezen"));

        QuestionModel question = questions
            .stream()
            .filter(q -> q.getOrder().equals(order))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Otázka nebyla nalezena"));

        repository.delete(question);

        questions
            .stream()
            .filter(q -> q.getOrder() > order)
            .forEach(q -> q.setOrder(q.getOrder() - 1));

        repository.saveAll(questions);
    }

    public QuestionModel get(UUID quizUuid, Integer order) {
        return repository
            .findByQuiz_Uuid(quizUuid)
            .orElseThrow(() -> new RuntimeException("Kvíz nebyl nalezen"))
            .stream()
            .filter(q -> q.getOrder().equals(order))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Otázka nebyla nalezena"));
    }

    public List<QuestionModel> getAll(UUID quizUuid) {
        return repository
            .findByQuiz_Uuid(quizUuid)
            .orElseThrow(() -> new RuntimeException("Kvíz nebyl nalezen"))
            .stream()
            .sorted(Comparator.comparing(QuestionModel::getOrder))
            .toList();
    }
}
