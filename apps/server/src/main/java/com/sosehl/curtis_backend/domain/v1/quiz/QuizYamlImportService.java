package com.sosehl.curtis_backend.domain.v1.quiz;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.sosehl.curtis_backend.domain.v1.question.Question;
import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.MatchingPair;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuestionYamlDto;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizYamlDto;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.web.multipart.MultipartFile;

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

    @Value("${quiz.media.path:./media}")
    private String mediaPathProperty;

    private Path importPath;
    private Path processedPath;
    private Path failedPath;
    private Path mediaPath;

    public QuizYamlImportService(
        QuizRepository quizRepository,
        Validator validator
    ) {
        this.quizRepository = quizRepository;
        this.validator = validator;
    }

    @PostConstruct
    void init() throws IOException {
        mediaPath = Paths.get(mediaPathProperty).toAbsolutePath().normalize();
        Files.createDirectories(mediaPath);
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
        UUID quizUuid;
        try {
            try (InputStream input = Files.newInputStream(file)) {
                quizUuid = importYaml(input);
            }
        } catch (Exception e) {
            moveToFailed(file, describe(e));
            return;
        }
        moveToProcessed(file, quizUuid);
    }

    public UUID importYaml(InputStream input) throws IOException {
        QuizYamlDto dto = yamlMapper.readValue(input, QuizYamlDto.class);
        validate(dto);
        return importQuiz(dto);
    }

    public UUID importUploaded(
        MultipartFile yaml,
        List<MultipartFile> images
    ) throws IOException {
        if (yaml == null || yaml.isEmpty()) {
            throw new IllegalArgumentException("YAML soubor musí být vyplněn");
        }

        QuizYamlDto dto;
        try (InputStream input = yaml.getInputStream()) {
            dto = yamlMapper.readValue(input, QuizYamlDto.class);
        }
        validate(dto);
        List<MultipartFile> attachments = images == null ? List.of() : images;
        Set<String> names = new java.util.HashSet<>();
        for (MultipartFile image : attachments) {
            String filename = safeFilename(image.getOriginalFilename());
            if (!names.add(filename)) {
                throw new IllegalArgumentException("Příloha se opakuje: " + filename);
            }
        }

        UUID quizUuid = importQuiz(dto);
        for (MultipartFile image : attachments) {
            String filename = safeFilename(image.getOriginalFilename());
            Files.copy(
                image.getInputStream(),
                mediaPath.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING
            );
        }
        return quizUuid;
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
            if (q.getImageRef() != null) safeFilename(q.getImageRef());
            if (q.getType() == QuestionType.MULTIPLE_CHOICE) {
                for (Integer idx : q.getCorrectIndexes()) {
                    if (
                        idx == null ||
                        idx < 0 ||
                        idx >= q.getOptions().size()
                    ) {
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
    }

    private String safeFilename(String filename) {
        if (
            filename == null ||
            filename.isBlank() ||
            filename.equals(".") ||
            filename.equals("..") ||
            filename.contains("/") ||
            filename.contains("\\") ||
            Paths.get(filename).getFileName() == null ||
            !filename.equals(Paths.get(filename).getFileName().toString())
        ) {
            throw new IllegalArgumentException("Název souboru obsahuje cestu");
        }
        return filename;
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
        question.setType(dto.getType());
        question.setPoints(dto.getPoints());
        question.setCodeSnippet(dto.getCodeSnippet());
        question.setImageRef(dto.getImageRef());
        question.setTimeInSeconds(dto.getTimeInSeconds());
        question.setQuiz(quiz);

        List<QuestionAnswer> answers = new ArrayList<>();
        if (dto.getType() == QuestionType.MULTIPLE_CHOICE) {
            for (int i = 0; i < dto.getOptions().size(); i++) {
                QuestionAnswer answer = new QuestionAnswer();
                answer.setAnswer(dto.getOptions().get(i));
                answer.setIsCorrect(dto.getCorrectIndexes().contains(i));
                answers.add(answer);
            }
        }
        question.setAnswers(answers);
        List<MatchingPair> pairs = dto.getPairs();
        question.setPairs(
            pairs == null ? new ArrayList<>() : new ArrayList<>(pairs)
        );
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
