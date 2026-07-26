package com.sosehl.curtis_backend.domain.v1.quiz;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.sosehl.curtis_backend.domain.v1.question.Question;
import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuestionYamlDto;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizYamlDto;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class QuizYamlImportService {

    private static final Logger log = LoggerFactory.getLogger(
        QuizYamlImportService.class
    );
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final QuizRepository quizRepository;
    private final Validator validator;
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @Value("${quiz.import.enabled:true}")
    private boolean importEnabled;

    @Value("${quiz.import.path:./import}")
    private String importPathProperty;

    private Path importPath;
    private Path processedPath;
    private Path failedPath;

    public QuizYamlImportService(
        QuizRepository quizRepository,
        Validator validator
    ) {
        this.quizRepository = quizRepository;
        this.validator = validator;
    }

    @PostConstruct
    void init() throws IOException {
        if (!importEnabled) {
            return;
        }

        importPath = Paths.get(importPathProperty);
        processedPath = importPath.resolve("processed");
        failedPath = importPath.resolve("failed");
        Files.createDirectories(processedPath);
        Files.createDirectories(failedPath);
    }

    @Scheduled(fixedDelay = 5000)
    public void pollImportFolder() {
        if (!importEnabled) {
            return;
        }

        try (Stream<Path> files = Files.list(importPath)) {
            files
                .filter(Files::isRegularFile)
                .filter(this::isYamlFile)
                .forEach(this::processFile);
        } catch (IOException e) {
            log.error(
                "Nepodařilo se přečíst import složku {}",
                importPath,
                e
            );
        }
    }

    public void processFile(Path file) {
        QuizYamlDto dto;
        try {
            dto = yamlMapper.readValue(file.toFile(), QuizYamlDto.class);
            validate(dto);
        } catch (Exception e) {
            moveToFailed(file, describe(e));
            return;
        }

        UUID quizUuid;
        try {
            quizUuid = importQuiz(dto);
        } catch (Exception e) {
            moveToFailed(file, describe(e));
            return;
        }

        moveToProcessed(file, quizUuid);
    }

    private boolean isYamlFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private void validate(QuizYamlDto dto) {
        Set<ConstraintViolation<QuizYamlDto>> violations = validator.validate(
            dto
        );
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(
                violations
                    .stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "))
            );
        }

        List<QuestionYamlDto> questions = dto.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            QuestionYamlDto q = questions.get(i);
            for (Integer idx : q.getCorrectIndexes()) {
                if (idx == null || idx < 0 || idx >= q.getOptions().size()) {
                    throw new IllegalArgumentException(
                        "Otázka " +
                        (i + 1) +
                        ": correctIndexes obsahuje neplatný index " +
                        idx
                    );
                }
            }
        }
    }

    private UUID importQuiz(QuizYamlDto dto) {
        Quiz quiz;
        if (dto.getUuid() != null) {
            quiz = quizRepository
                .findByUuid(dto.getUuid())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Kvíz s UUID " + dto.getUuid() + " neexistuje"
                    )
                );
        } else {
            quiz = new Quiz();
            quiz.setCreatedAt(LocalDateTime.now());
        }

        quiz.setTitle(dto.getTitle());
        quiz.setDescription(dto.getDescription());
        quiz.setMaxQuestionsPerSession(dto.getMaxQuestionsPerSession());
        quiz.setShuffle(dto.getShuffle());
        quiz.setEditedAt(LocalDateTime.now());
        List<Question> newQuestions = dto
            .getQuestions()
            .stream()
            .map(q -> toQuestion(q, quiz))
            .collect(Collectors.toCollection(ArrayList::new));
        quiz.setQuestions(newQuestions);

        return quizRepository.save(quiz).getUuid();
    }

    private Question toQuestion(QuestionYamlDto dto, Quiz quiz) {
        Question question = new Question();
        question.setQuestion(dto.getQuestion());
        question.setTimeInSeconds(dto.getTimeInSeconds());
        question.setQuiz(quiz);

        List<QuestionAnswer> answers = new ArrayList<>();
        for (int i = 0; i < dto.getOptions().size(); i++) {
            QuestionAnswer answer = new QuestionAnswer();
            answer.setAnswer(dto.getOptions().get(i));
            answer.setIsCorrect(dto.getCorrectIndexes().contains(i));
            answers.add(answer);
        }
        question.setAnswers(answers);
        return question;
    }

    private void moveToProcessed(Path file, UUID quizUuid) {
        try {
            Path target = processedPath.resolve(timestampedName(file));
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            log.info(
                "Import {} proběhl úspěšně, quizUuid={}",
                file.getFileName(),
                quizUuid
            );
        } catch (IOException e) {
            log.error(
                "Import {} proběhl úspěšně (quizUuid={}), ale soubor se nepodařilo přesunout",
                file.getFileName(),
                quizUuid,
                e
            );
        }
    }

    private void moveToFailed(Path file, String reason) {
        try {
            String name = timestampedName(file);
            Files.move(
                file,
                failedPath.resolve(name),
                StandardCopyOption.REPLACE_EXISTING
            );
            Files.writeString(failedPath.resolve(name + ".error.txt"), reason);
            log.warn("Import {} selhal: {}", file.getFileName(), reason);
        } catch (IOException e) {
            log.error(
                "Nepodařilo se přesunout selhaný soubor {}",
                file.getFileName(),
                e
            );
        }
    }

    private String timestampedName(Path file) {
        return (
            TIMESTAMP_FORMAT.format(LocalDateTime.now()) +
            "-" +
            file.getFileName()
        );
    }

    private String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.toString();
    }
}
