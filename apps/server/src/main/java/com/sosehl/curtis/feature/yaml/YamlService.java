package com.sosehl.curtis.feature.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.quizzes.core.QuizAuthoring;
import com.sosehl.curtis.feature.quizzes.core.QuizStatus;
import com.sosehl.curtis.feature.yaml.dto.YamlQuizDocument;
import com.sosehl.curtis.feature.yaml.dto.YamlQuizDocument.YamlOptionDocument;
import com.sosehl.curtis.feature.yaml.dto.YamlQuizDocument.YamlPairDocument;
import com.sosehl.curtis.feature.yaml.dto.YamlQuizDocument.YamlQuestionDocument;
import com.sosehl.curtis.shared.errors.ProblemException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.LoaderOptions;

@Service
public class YamlService {

    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_YAML_BYTES = 1_048_576;
    private static final int MAX_YAML_DEPTH = 50;
    private static final int MAX_YAML_ALIASES = 20;

    private final QuizAuthoring quizzes;
    private final Validator validator;
    private final YAMLMapper yaml;

    public YamlService(QuizAuthoring quizzes, Validator validator) {
        this.quizzes = quizzes;
        this.validator = validator;
        this.yaml = createMapper();
    }

    public YamlQuizDocument parse(byte[] content) {
        if (content == null || content.length == 0) throw invalid("YAML is required");
        if (content.length > MAX_YAML_BYTES) throw invalid("YAML exceeds 1 MiB");
        try {
            YamlQuizDocument document = yaml.readValue(content, YamlQuizDocument.class);
            requireValidDocument(document);
            return document;
        } catch (ProblemException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw invalid("YAML is malformed: " + exception.getOriginalMessage());
        } catch (IOException exception) {
            throw invalid("YAML could not be read");
        }
    }

    public QuizAuthoring.Quiz createForTeacher(
        UUID teacherId,
        YamlQuizDocument document,
        Map<String, UUID> packageAssets
    ) {
        if (document.quizId() != null || document.version() != null) {
            throw invalid("New YAML must not contain quizId or version");
        }
        return quizzes.create(
            teacherId,
            toDraft(document, null, packageAssets)
        );
    }

    public QuizAuthoring.Quiz replaceForTeacher(
        UUID teacherId,
        UUID quizId,
        YamlQuizDocument document
    ) {
        if (document.quizId() != null && !document.quizId().equals(quizId)) {
            throw invalid("YAML quizId does not match the request path");
        }
        if (document.version() == null) {
            throw invalid("YAML version is required when replacing a quiz");
        }
        return quizzes.replace(
            teacherId,
            quizId,
            toDraft(document, document.version(), Map.of())
        );
    }

    public byte[] exportForTeacher(UUID teacherId, UUID quizId) {
        QuizAuthoring.Quiz response = quizzes.get(teacherId, quizId);
        try {
            return yaml.writeValueAsString(toDocument(response))
                .getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize quiz YAML", exception);
        }
    }

    private QuizAuthoring.Draft toDraft(
        YamlQuizDocument document,
        Long expectedVersion,
        Map<String, UUID> packageAssets
    ) {
        List<QuizAuthoring.QuestionDraft> questions = new ArrayList<>();
        for (YamlQuestionDocument question : document.questions()) {
            List<QuizAuthoring.OptionDraft> options = safe(question.options()).stream()
                .map(value -> new QuizAuthoring.OptionDraft(
                    value.id(),
                    value.text(),
                    Boolean.TRUE.equals(value.correct())
                ))
                .toList();
            List<QuizAuthoring.PairDraft> pairs = safe(question.pairs()).stream()
                .map(value -> new QuizAuthoring.PairDraft(
                    value.id(),
                    value.left(),
                    value.right()
                ))
                .toList();
            questions.add(
                new QuizAuthoring.QuestionDraft(
                    question.id(),
                    question.type() == null ? QuestionType.MULTIPLE_CHOICE : question.type(),
                    question.prompt(),
                    question.points() == null ? 1 : question.points(),
                    question.codeSnippet(),
                    resolveMedia(question.image(), packageAssets),
                    question.timeSeconds() == null ? 30 : question.timeSeconds(),
                    options,
                    pairs
                )
            );
        }
        return new QuizAuthoring.Draft(
            document.title(),
            document.description(),
            document.subjectId(),
            document.chapter(),
            document.status() == null ? QuizStatus.DRAFT : document.status(),
            document.maxQuestionsPerSession(),
            Boolean.TRUE.equals(document.shuffle()),
            document.validFrom(),
            document.validTo(),
            expectedVersion,
            questions
        );
    }

    private UUID resolveMedia(String image, Map<String, UUID> packageAssets) {
        if (image == null || image.isBlank()) return null;
        UUID packaged = packageAssets.get(image);
        if (packaged != null) return packaged;
        if (!packageAssets.isEmpty()) {
            throw invalid("YAML references a missing package asset: " + image);
        }
        try {
            return UUID.fromString(image);
        } catch (IllegalArgumentException exception) {
            throw invalid("Web YAML image values must be uploaded media UUIDs");
        }
    }

    private YamlQuizDocument toDocument(QuizAuthoring.Quiz response) {
        return new YamlQuizDocument(
            SCHEMA_VERSION,
            null,
            response.id(),
            response.version(),
            response.title(),
            response.description(),
            response.subjectId(),
            response.chapter(),
            response.status(),
            response.maxQuestionsPerSession(),
            response.shuffle(),
            response.validFrom(),
            response.validTo(),
            response.questions().stream().map(this::toDocument).toList()
        );
    }

    private YamlQuestionDocument toDocument(QuizAuthoring.Question question) {
        return new YamlQuestionDocument(
            question.id(),
            question.type(),
            question.prompt(),
            question.points(),
            question.codeSnippet(),
            question.mediaId() == null ? null : question.mediaId().toString(),
            question.timeSeconds(),
            question.options().stream().map(this::toDocument).toList(),
            question.pairs().stream().map(this::toDocument).toList()
        );
    }

    private YamlOptionDocument toDocument(QuizAuthoring.Option option) {
        return new YamlOptionDocument(option.id(), option.text(), option.correct());
    }

    private YamlPairDocument toDocument(QuizAuthoring.Pair pair) {
        return new YamlPairDocument(pair.id(), pair.left(), pair.right());
    }

    private void requireValidDocument(YamlQuizDocument document) {
        if (document == null) throw invalid("YAML document is empty");
        if (document.schemaVersion() != null &&
            document.schemaVersion() != SCHEMA_VERSION) {
            throw invalid("Unsupported YAML schemaVersion");
        }
        ConstraintViolation<YamlQuizDocument> violation = validator
            .validate(document)
            .stream()
            .min(Comparator.comparing(value -> value.getPropertyPath().toString()))
            .orElse(null);
        if (violation != null) {
            String path = violation.getPropertyPath().toString();
            String prefix = path.isBlank() ? "" : path + " ";
            throw invalid(prefix + violation.getMessage());
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static YAMLMapper createMapper() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        loaderOptions.setAllowRecursiveKeys(false);
        loaderOptions.setMaxAliasesForCollections(MAX_YAML_ALIASES);
        loaderOptions.setNestingDepthLimit(MAX_YAML_DEPTH);
        loaderOptions.setCodePointLimit(MAX_YAML_BYTES);
        YAMLFactory factory = YAMLFactory.builder()
            .loaderOptions(loaderOptions)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build();
        return YAMLMapper.builder(factory)
            .findAndAddModules()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    }

    private static ProblemException invalid(String message) {
        return ProblemException.badRequest("quiz_yaml_invalid", message);
    }
}
