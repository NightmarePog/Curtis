package com.sosehl.curtis.feature.yaml.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis.feature.media.core.MediaWriter;
import com.sosehl.curtis.feature.quizzes.core.QuizAuthoring;
import com.sosehl.curtis.feature.quizzes.core.QuizStatus;
import com.sosehl.curtis.shared.errors.ProblemException;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.feature.users.core.UserSummary;
import com.sosehl.curtis.feature.yaml.YamlImportProperties;
import com.sosehl.curtis.feature.yaml.dto.YamlQuizDocument;
import com.sosehl.curtis.feature.yaml.YamlService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class YamlPackageImporterTest {

    @TempDir
    Path root;

    private final YamlService yaml = mock(YamlService.class);
    private final MediaWriter media = mock(MediaWriter.class);
    private final UserDirectory users = mock(UserDirectory.class);
    private final YamlImportJobRepository jobs = mock(YamlImportJobRepository.class);
    private final AtomicReference<YamlImportJobEntity> savedJob = new AtomicReference<>();
    private YamlPackageImporter importer;

    @BeforeEach
    void setUp() throws Exception {
        when(jobs.findByContentDigest(any())).thenAnswer(invocation ->
            Optional.ofNullable(savedJob.get())
        );
        when(jobs.saveAndFlush(any())).thenAnswer(invocation -> {
            YamlImportJobEntity job = invocation.getArgument(0);
            savedJob.set(job);
            return job;
        });
        importer = new YamlPackageImporter(
            yaml,
            media,
            users,
            jobs,
            new ObjectMapper(),
            new TransactionTemplate(transactionManager()),
            Clock.fixed(Instant.parse("2026-09-02T12:00:00Z"), ZoneOffset.UTC),
            new YamlImportProperties(true, root, 5000, 5, 1024 * 1024)
        );
        importer.initialize();
    }

    @Test
    void readyPackageIsCreateOnlyAndDigestIdempotent() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        YamlQuizDocument document = document("teacher@school.cz");
        when(yaml.parse(any())).thenReturn(document);
        when(users.requireActiveTeacherByUsername("teacher@school.cz"))
            .thenReturn(new UserSummary(
                ownerId,
                "Teacher",
                "teacher@school.cz",
                Set.of(UserRole.TEACHER)
            ));
        when(yaml.createForTeacher(eq(ownerId), eq(document), any()))
            .thenReturn(response(quizId, ownerId));
        Path first = packageDirectory("job-one", "same-content");

        importer.processPackage(first);

        assertThat(root.resolve("processed/job-one")).isDirectory();
        assertThat(savedJob.get().getStatus()).isEqualTo(YamlImportJobStatus.COMPLETED);
        Path duplicate = packageDirectory("job-two", "same-content");
        importer.processPackage(duplicate);
        assertThat(root.resolve("processed/job-two")).isDirectory();
        verify(yaml, times(1)).createForTeacher(eq(ownerId), eq(document), any());
    }

    @Test
    void unknownOwnerMovesPackageToFailedWithStructuredReport() throws Exception {
        YamlQuizDocument document = document("missing@school.cz");
        when(yaml.parse(any())).thenReturn(document);
        when(users.requireActiveTeacherByUsername("missing@school.cz"))
            .thenThrow(ProblemException.notFound("teacher_not_found", "Teacher not found."));
        Path directory = packageDirectory("unknown-owner", "unknown-owner-content");

        importer.processPackage(directory);

        Path failed = root.resolve("failed/unknown-owner");
        assertThat(failed).isDirectory();
        assertThat(failed.resolve("error.json")).content()
            .contains("YAML_IMPORT_FAILED")
            .contains("Teacher not found");
        assertThat(savedJob.get().getStatus()).isEqualTo(YamlImportJobStatus.FAILED);
    }

    private Path packageDirectory(String name, String yamlContent) throws Exception {
        Path directory = Files.createDirectories(root.resolve("incoming").resolve(name));
        Files.writeString(directory.resolve("quiz.yaml"), yamlContent);
        Files.createFile(directory.resolve(".ready"));
        return directory;
    }

    private YamlQuizDocument document(String ownerUsername) {
        return new YamlQuizDocument(
            1,
            ownerUsername,
            null,
            null,
            "Imported quiz",
            null,
            UUID.randomUUID(),
            null,
            QuizStatus.DRAFT,
            1,
            false,
            null,
            null,
            List.of(new YamlQuizDocument.YamlQuestionDocument(
                null,
                null,
                "Question",
                1,
                null,
                null,
                30,
                List.of(),
                List.of()
            ))
        );
    }

    private QuizAuthoring.Quiz response(UUID quizId, UUID ownerId) {
        return new QuizAuthoring.Quiz(
            quizId,
            0,
            ownerId,
            UUID.randomUUID(),
            "Subject",
            "Imported quiz",
            null,
            null,
            QuizStatus.DRAFT,
            1,
            false,
            null,
            null,
            Instant.EPOCH,
            Instant.EPOCH,
            null,
            List.of()
        );
    }

    private PlatformTransactionManager transactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {}

            @Override
            public void rollback(TransactionStatus status) {}
        };
    }
}
