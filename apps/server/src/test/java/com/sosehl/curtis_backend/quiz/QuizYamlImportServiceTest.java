package com.sosehl.curtis_backend.quiz;

import static org.assertj.core.api.Assertions.*;

import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizRepository;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizYamlImportService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class QuizYamlImportServiceTest {

    @TempDir
    static Path importDir;

    @DynamicPropertySource
    static void importProperties(DynamicPropertyRegistry registry) {
        registry.add("quiz.import.enabled", () -> "true");
        registry.add("quiz.import.path", importDir::toString);
    }

    @Autowired
    private QuizYamlImportService importService;

    @Autowired
    private QuizRepository quizRepository;

    private Path writeYaml(String filename, String content)
        throws IOException {
        Path file = importDir.resolve(filename);
        Files.writeString(file, content);
        return file;
    }

    private long countFilesIn(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.count();
        }
    }

    // importDir is a static @TempDir shared across every test method in
    // this class (required so @DynamicPropertySource can read it before
    // the Spring context starts). Clear the processed/failed subfolders
    // before each test so file-count assertions don't depend on
    // execution order or accumulate leftovers from earlier tests.
    @BeforeEach
    void cleanImportSubDirectories() throws IOException {
        clearDirectory(importDir.resolve("processed"));
        clearDirectory(importDir.resolve("failed"));
    }

    private void clearDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.list(dir)) {
            for (Path path : stream.toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void shouldImportValidQuizAndMoveToProcessed() throws IOException {
        Path file = writeYaml(
            "valid.yaml",
            """
            title: "Chapter 5 Quiz"
            description: "Cell biology basics"
            shuffle: true
            maxQuestionsPerSession: 10
            questions:
              - question: "What is the powerhouse of the cell?"
                timeInSeconds: 20
                options: ["Mitochondria", "Nucleus", "Ribosome"]
                correctIndexes: [0]
            """
        );

        importService.processFile(file);

        assertThat(Files.exists(file)).isFalse();
        assertThat(countFilesIn(importDir.resolve("processed"))).isEqualTo(1);

        Optional<Quiz> saved = quizRepository
            .findAll()
            .stream()
            .filter(q -> "Chapter 5 Quiz".equals(q.getTitle()))
            .findFirst();
        assertThat(saved).isPresent();
        assertThat(saved.get().getQuestions()).hasSize(1);
        assertThat(saved.get().getQuestions().get(0).getAnswers()).hasSize(3);
    }

    @Test
    void shouldMoveMalformedYamlToFailedWithErrorFile() throws IOException {
        Path file = writeYaml(
            "broken.yaml",
            "title: [this is not a valid quiz structure\n"
        );

        importService.processFile(file);

        assertThat(Files.exists(file)).isFalse();
        assertThat(countFilesIn(importDir.resolve("failed"))).isEqualTo(2);
    }

    @Test
    void shouldFailValidationWhenNoCorrectAnswerMarked() throws IOException {
        Path file = writeYaml(
            "no-correct.yaml",
            """
            title: "Bad Quiz"
            maxQuestionsPerSession: 5
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                options: ["A", "B"]
                correctIndexes: []
            """
        );

        importService.processFile(file);

        assertThat(Files.exists(file)).isFalse();
        assertThat(countFilesIn(importDir.resolve("failed"))).isEqualTo(2);
    }

    @Test
    void shouldFailWhenCorrectIndexOutOfRange() throws IOException {
        Path file = writeYaml(
            "bad-index.yaml",
            """
            title: "Bad Index Quiz"
            maxQuestionsPerSession: 5
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                options: ["A", "B"]
                correctIndexes: [5]
            """
        );

        importService.processFile(file);

        assertThat(countFilesIn(importDir.resolve("failed"))).isEqualTo(2);
    }

    @Test
    void shouldFailWhenReplaceTargetUuidDoesNotExist() throws IOException {
        Path file = writeYaml(
            "unknown-uuid.yaml",
            """
            uuid: "%s"
            title: "Replacement Quiz"
            maxQuestionsPerSession: 5
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                options: ["A", "B"]
                correctIndexes: [0]
            """.formatted(UUID.randomUUID())
        );

        importService.processFile(file);

        assertThat(countFilesIn(importDir.resolve("failed"))).isEqualTo(2);
    }

    @Test
    void shouldReplaceExistingQuizWhenUuidMatches() throws IOException {
        Quiz existing = new Quiz();
        existing.setTitle("Old Title");
        existing.setMaxQuestionsPerSession(5);
        existing.setShuffle(false);
        UUID uuid = quizRepository.save(existing).getUuid();

        Path file = writeYaml(
            "replace.yaml",
            """
            uuid: "%s"
            title: "New Title"
            maxQuestionsPerSession: 3
            questions:
              - question: "Pick one"
                timeInSeconds: 10
                options: ["A", "B"]
                correctIndexes: [0]
            """.formatted(uuid)
        );

        importService.processFile(file);

        Quiz updated = quizRepository.findByUuid(uuid).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getMaxQuestionsPerSession()).isEqualTo(3);
        assertThat(updated.getQuestions()).hasSize(1);
        assertThat(countFilesIn(importDir.resolve("processed"))).isEqualTo(1);
    }
}
