package com.sosehl.curtis.feature.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.quizzes.core.QuizAuthoring;
import com.sosehl.curtis.feature.quizzes.core.QuizStatus;
import com.sosehl.curtis.feature.yaml.dto.YamlQuizDocument;
import com.sosehl.curtis.shared.errors.ProblemException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class YamlServiceTest {

    private static final Validator VALIDATOR = Validation
        .buildDefaultValidatorFactory()
        .getValidator();

    private final QuizAuthoring quizzes = mock(QuizAuthoring.class);
    private YamlService yaml;

    @BeforeEach
    void setUp() {
        yaml = new YamlService(quizzes, VALIDATOR);
    }

    @Test
    void parsesHumanReadableOrderedOptions() {
        UUID teacherId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        YamlQuizDocument document = yaml.parse(("""
            schemaVersion: 1
            title: Network quiz
            subjectId: %s
            status: PUBLISHED
            maxQuestionsPerSession: 1
            questions:
              - type: MULTIPLE_CHOICE
                prompt: Secure protocol?
                points: 2
                timeInSeconds: 20
                options:
                  - text: HTTPS
                    correct: true
                  - text: HTTP
                    correct: false
            """).formatted(subjectId).getBytes(StandardCharsets.UTF_8));

        yaml.createForTeacher(teacherId, document, Map.of());

        ArgumentCaptor<QuizAuthoring.Draft> request = ArgumentCaptor.forClass(
            QuizAuthoring.Draft.class
        );
        verify(quizzes).create(eq(teacherId), request.capture());
        assertThat(request.getValue().questions().get(0).timeSeconds()).isEqualTo(20);
        assertThat(request.getValue().questions().get(0).options())
            .extracting("text", "correct")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("HTTPS", true),
                org.assertj.core.groups.Tuple.tuple("HTTP", false)
            );
    }

    @Test
    void rejectsUnknownKeyInsteadOfSilentlyIgnoringTypo() {
        UUID subjectId = UUID.randomUUID();
        byte[] content = ("""
            title: Typo quiz
            subjectId: %s
            maxQuestionsPerSession: 1
            shufle: true
            questions:
              - prompt: Question
            """).formatted(subjectId).getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> yaml.parse(content))
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("quiz_yaml_invalid")
            );
    }

    @Test
    void rejectsDuplicateKeys() {
        UUID subjectId = UUID.randomUUID();
        byte[] content = ("""
            title: First title
            title: Second title
            subjectId: %s
            maxQuestionsPerSession: 1
            questions:
              - prompt: Question
            """).formatted(subjectId).getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> yaml.parse(content))
            .isInstanceOfSatisfying(ProblemException.class, exception -> {
                assertThat(exception.code()).isEqualTo("quiz_yaml_invalid");
                assertThat(exception.getMessage()).contains("Duplicate field");
            });
    }

    @Test
    void enforcesAnnotatedNestedLimitsDuringParsing() {
        UUID subjectId = UUID.randomUUID();
        byte[] content = (("""
            title: Invalid timer
            subjectId: %s
            maxQuestionsPerSession: 1
            questions:
              - prompt: Question
                timeSeconds: 3601
            """).formatted(subjectId)).getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> yaml.parse(content))
            .isInstanceOfSatisfying(ProblemException.class, exception -> {
                assertThat(exception.code()).isEqualTo("quiz_yaml_invalid");
                assertThat(exception.getMessage()).contains("timeSeconds");
            });
    }

    @Test
    void exportKeepsIdentityVersionAndAnswerKey() {
        UUID teacherId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        QuizAuthoring.Quiz response = new QuizAuthoring.Quiz(
            quizId,
            4,
            teacherId,
            subjectId,
            "Programming",
            "Quiz",
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
            List.of(new QuizAuthoring.Question(
                questionId,
                0,
                QuestionType.MULTIPLE_CHOICE,
                "Question",
                1,
                null,
                null,
                30,
                List.of(new QuizAuthoring.Option(optionId, 0, "Correct", true)),
                List.of()
            ))
        );
        when(quizzes.get(teacherId, quizId)).thenReturn(response);

        YamlQuizDocument exported = yaml.parse(yaml.exportForTeacher(teacherId, quizId));

        assertThat(exported.quizId()).isEqualTo(quizId);
        assertThat(exported.version()).isEqualTo(4);
        assertThat(exported.questions().get(0).id()).isEqualTo(questionId);
        assertThat(exported.questions().get(0).options().get(0).correct()).isTrue();
    }
}
