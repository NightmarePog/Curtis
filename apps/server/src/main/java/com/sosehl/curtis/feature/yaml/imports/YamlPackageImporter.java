package com.sosehl.curtis.feature.yaml.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sosehl.curtis.feature.media.core.MediaWriter;
import com.sosehl.curtis.feature.quizzes.core.QuizAuthoring;
import com.sosehl.curtis.feature.yaml.YamlImportProperties;
import com.sosehl.curtis.feature.yaml.YamlService;
import com.sosehl.curtis.feature.yaml.dto.YamlQuizDocument;
import com.sosehl.curtis.shared.errors.ProblemException;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserSummary;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class YamlPackageImporter {

    private static final Logger log = LoggerFactory.getLogger(YamlPackageImporter.class);

    private record PackageContent(
        Path directory,
        byte[] yaml,
        List<Path> assets,
        String digest
    ) {}

    private final YamlService yaml;
    private final MediaWriter media;
    private final UserDirectory users;
    private final YamlImportJobRepository jobs;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final boolean enabled;
    private final Path incoming;
    private final Path processed;
    private final Path failed;
    private final int maxAssets;
    private final long maxTotalAssetBytes;

    YamlPackageImporter(
        YamlService yaml,
        MediaWriter media,
        UserDirectory users,
        YamlImportJobRepository jobs,
        ObjectMapper json,
        TransactionTemplate transactions,
        Clock clock,
        YamlImportProperties properties
    ) {
        this.yaml = yaml;
        this.media = media;
        this.users = users;
        this.jobs = jobs;
        this.json = json;
        this.transactions = transactions;
        this.clock = clock;
        this.enabled = properties.enabled();
        Path normalizedRoot = properties.root().toAbsolutePath().normalize();
        this.incoming = normalizedRoot.resolve("incoming");
        this.processed = normalizedRoot.resolve("processed");
        this.failed = normalizedRoot.resolve("failed");
        this.maxAssets = properties.maxAssets();
        this.maxTotalAssetBytes = properties.maxTotalAssetBytes();
    }

    @PostConstruct
    void initialize() throws IOException {
        if (!enabled) return;
        Files.createDirectories(incoming);
        Files.createDirectories(processed);
        Files.createDirectories(failed);
    }

    @Scheduled(fixedDelayString = "${app.quiz-import.poll-ms:5000}")
    public void poll() {
        if (!enabled) return;
        try (DirectoryStream<Path> directories = Files.newDirectoryStream(incoming)) {
            for (Path directory : directories) {
                if (isReadyPackage(directory)) processPackage(directory);
            }
        } catch (IOException exception) {
            log.error("Could not scan YAML package directory {}", incoming, exception);
        }
    }

    public void processPackage(Path directory) {
        PackageContent content = null;
        try {
            requireDirectChild(directory);
            content = readPackage(directory);
            PackageContent discovered = content;
            YamlImportJobEntity previous = transactions.execute(status ->
                jobs.findByContentDigest(discovered.digest()).orElse(null)
            );
            if (previous != null) {
                finishDuplicate(content.directory(), previous);
                return;
            }

            YamlQuizDocument document = yaml.parse(content.yaml());
            if (document.ownerUsername() == null || document.ownerUsername().isBlank()) {
                throw invalid("ownerUsername is required for watched imports");
            }
            if (document.quizId() != null || document.version() != null) {
                throw invalid("Watched packages are create-only");
            }
            UserSummary owner = users.requireActiveTeacherByUsername(
                document.ownerUsername().trim()
            );
            PackageContent captured = content;
            YamlImportJobEntity completed = transactions.execute(status ->
                importTransaction(captured, document, owner)
            );
            movePackage(directory, processed);
            log.info(
                "Imported YAML package {} as quiz {}",
                directory.getFileName(),
                completed == null ? null : completed.getQuizId()
            );
        } catch (Exception exception) {
            String digest = content == null ? fallbackDigest(directory) : content.digest();
            JsonNode error = errorDetails(exception);
            recordFailure(directory, digest, error);
            moveFailed(directory, error);
            log.warn("YAML package {} failed: {}", directory.getFileName(), safeMessage(exception));
        }
    }

    private YamlImportJobEntity importTransaction(
        PackageContent content,
        YamlQuizDocument document,
        UserSummary owner
    ) {
        YamlImportJobEntity existing = jobs.findByContentDigest(content.digest()).orElse(null);
        if (existing != null) return existing;
        Instant now = clock.instant();
        YamlImportJobEntity job = jobs.saveAndFlush(
            new YamlImportJobEntity(
                UUID.randomUUID(),
                owner.id(),
                content.directory().toString(),
                content.digest(),
                now
            )
        );
        Map<String, UUID> assets = new LinkedHashMap<>();
        for (Path asset : content.assets()) {
            try {
                UUID mediaId = media.storeForImport(
                    owner.id(),
                    asset.getFileName().toString(),
                    Files.newInputStream(asset)
                );
                assets.put(asset.getFileName().toString(), mediaId);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not store asset", exception);
            }
        }
        QuizAuthoring.Quiz quiz = yaml.createForTeacher(
            owner.id(),
            document,
            assets
        );
        job.complete(quiz.id(), clock.instant());
        return jobs.saveAndFlush(job);
    }

    private PackageContent readPackage(Path directory) throws IOException {
        Path quizFile = directory.resolve("quiz.yaml");
        requireRegularFile(quizFile, "quiz.yaml is required");
        byte[] yamlBytes = Files.readAllBytes(quizFile);
        if (yamlBytes.length > YamlService.MAX_YAML_BYTES) {
            throw invalid("quiz.yaml exceeds 1 MiB");
        }
        List<Path> assets = new ArrayList<>();
        Path assetDirectory = directory.resolve("assets");
        long totalBytes = 0;
        if (Files.exists(assetDirectory, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isDirectory(assetDirectory, LinkOption.NOFOLLOW_LINKS)) {
                throw invalid("assets must be a directory");
            }
            try (DirectoryStream<Path> files = Files.newDirectoryStream(assetDirectory)) {
                for (Path file : files) {
                    requireRegularFile(file, "assets may contain only regular files");
                    assets.add(file);
                    totalBytes = Math.addExact(totalBytes, Files.size(file));
                    if (assets.size() > maxAssets) throw invalid("Package has too many assets");
                    if (totalBytes > maxTotalAssetBytes) {
                        throw invalid("Package assets exceed the total size limit");
                    }
                }
            }
        }
        assets.sort(Comparator.comparing(path -> path.getFileName().toString()));
        return new PackageContent(directory, yamlBytes, List.copyOf(assets), digest(yamlBytes, assets));
    }

    private String digest(byte[] yamlBytes, List<Path> assets) throws IOException {
        MessageDigest digest = sha256();
        digest.update(yamlBytes);
        digest.update((byte) 0);
        byte[] buffer = new byte[8192];
        for (Path asset : assets) {
            digest.update(asset.getFileName().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update((byte) 0);
            try (InputStream input = new DigestInputStream(Files.newInputStream(asset), digest)) {
                while (input.read(buffer) != -1) {
                    // DigestInputStream updates the digest.
                }
            }
            digest.update((byte) 0);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private String fallbackDigest(Path directory) {
        return HexFormat.of().formatHex(
            sha256().digest(directory.toAbsolutePath().normalize().toString()
                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
    }

    private void recordFailure(Path directory, String digest, JsonNode error) {
        try {
            transactions.executeWithoutResult(status -> {
                YamlImportJobEntity job = jobs.findByContentDigest(digest)
                    .orElseGet(() -> new YamlImportJobEntity(
                        UUID.randomUUID(),
                        null,
                        directory.toString(),
                        digest,
                        clock.instant()
                    ));
                if (job.getStatus() != YamlImportJobStatus.COMPLETED) {
                    job.fail(error, clock.instant());
                    jobs.saveAndFlush(job);
                }
            });
        } catch (DataIntegrityViolationException exception) {
            log.debug("Import failure was already recorded for digest {}", digest);
        }
    }

    private void finishDuplicate(Path directory, YamlImportJobEntity previous) {
        if (previous.getStatus() == YamlImportJobStatus.COMPLETED) {
            movePackage(directory, processed);
        } else if (previous.getStatus() == YamlImportJobStatus.FAILED) {
            moveFailed(directory, previous.getErrorDetails());
        }
    }

    private boolean isReadyPackage(Path directory) {
        return Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) &&
            Files.isRegularFile(directory.resolve(".ready"), LinkOption.NOFOLLOW_LINKS);
    }

    private void requireDirectChild(Path directory) {
        Path normalized = directory.toAbsolutePath().normalize();
        if (!normalized.getParent().equals(incoming)) {
            throw invalid("Import package must be a direct child of incoming");
        }
    }

    private void requireRegularFile(Path file, String message) {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw invalid(message);
    }

    private void moveFailed(Path directory, JsonNode error) {
        Path target = movePackage(directory, failed);
        if (target == null) return;
        try {
            Files.writeString(
                target.resolve("error.json"),
                json.writerWithDefaultPrettyPrinter().writeValueAsString(error)
            );
        } catch (IOException exception) {
            log.error("Could not write import error report to {}", target, exception);
        }
    }

    private Path movePackage(Path directory, Path destination) {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return null;
        String name = directory.getFileName().toString();
        Path target = destination.resolve(name).normalize();
        if (!target.getParent().equals(destination)) {
            log.error("Refusing to move unsafe import package name {}", name);
            return null;
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            target = destination.resolve(name + "-" + UUID.randomUUID());
        }
        try {
            try {
                return Files.move(directory, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                return Files.move(directory, target);
            }
        } catch (IOException exception) {
            log.error("Could not move import package {} to {}", directory, destination, exception);
            return null;
        }
    }

    private JsonNode errorDetails(Exception exception) {
        ObjectNode details = json.createObjectNode();
        details.put("code", "YAML_IMPORT_FAILED");
        details.put("message", safeMessage(exception));
        details.put("timestamp", clock.instant().toString());
        return details;
    }

    private String safeMessage(Exception exception) {
        Throwable value = exception;
        while (value.getCause() != null && value instanceof IllegalStateException) {
            value = value.getCause();
        }
        String message = value.getMessage();
        if (message == null || message.isBlank()) return "Import failed";
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static ProblemException invalid(String message) {
        return ProblemException.badRequest("yaml_import_invalid", message);
    }
}
