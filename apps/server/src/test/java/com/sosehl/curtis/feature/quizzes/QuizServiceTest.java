package com.sosehl.curtis.feature.quizzes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.sosehl.curtis.feature.media.core.MediaUsagePolicy;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.OptionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuestionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizWriteRequest;
import com.sosehl.curtis.feature.quizzes.authoring.QuizRequestValidator;
import com.sosehl.curtis.feature.quizzes.core.OptionSnapshot;
import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.quizzes.core.QuizAuthoring;
import com.sosehl.curtis.feature.quizzes.core.QuizChangeNotifier;
import com.sosehl.curtis.feature.quizzes.core.QuizSnapshot;
import com.sosehl.curtis.feature.quizzes.core.QuizStatus;
import com.sosehl.curtis.feature.subjects.core.SubjectCatalog;
import com.sosehl.curtis.feature.subjects.core.SubjectSummary;
import com.sosehl.curtis.shared.errors.ProblemException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuizServiceTest {

    private final QuizRepository repository = mock(QuizRepository.class);
    private final SubjectCatalog subjects = mock(SubjectCatalog.class);
    private final MediaUsagePolicy mediaUsagePolicy = mock(MediaUsagePolicy.class);
    private final QuizChangeNotifier changes = mock(QuizChangeNotifier.class);
    private final Validator validator = Validation
        .buildDefaultValidatorFactory()
        .getValidator();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-02T12:00:00Z");
    private final AtomicReference<QuizEntity> saved = new AtomicReference<>();
    private QuizService service;

    @BeforeEach
    void setUp() {
        service = new QuizService(
            repository,
            subjects,
            Clock.fixed(now, ZoneOffset.UTC),
            changes,
            new QuizRequestValidator(validator, mediaUsagePolicy)
        );
        when(subjects.require(subjectId)).thenReturn(
            new SubjectSummary(subjectId, "PRG", "Programming", true, 0)
        );
        when(repository.saveAndFlush(any(QuizEntity.class))).thenAnswer(invocation -> {
            QuizEntity entity = invocation.getArgument(0);
            saved.set(entity);
            return entity;
        });
    }

    @Test
    void createsOwnedOrderedQuizAndLaunchSnapshot() {
        QuizResponse created = service.createForTeacher(
            teacherId,
            validRequest(QuizStatus.PUBLISHED)
        );
        when(repository.findById(created.id())).thenReturn(Optional.of(saved.get()));

        QuizSnapshot snapshot = service.loadLaunchable(created.id(), teacherId);

        assertThat(snapshot.title()).isEqualTo("Network basics");
        assertThat(snapshot.subjectName()).isEqualTo("Programming");
        assertThat(snapshot.questions()).hasSize(1);
        assertThat(snapshot.questions().get(0).position()).isZero();
        assertThat(snapshot.questions().get(0).options())
            .extracting(OptionSnapshot::position)
            .containsExactly(0, 1);
        assertThat(snapshot.questions().get(0).options())
            .filteredOn(OptionSnapshot::correct)
            .hasSize(1);
        verify(subjects, times(2)).requireTeacherAssignment(teacherId, subjectId);
    }

    @Test
    void authoringBoundaryPreservesNestedFieldsAndNormalizesText() {
        UUID questionId = UUID.randomUUID();
        UUID correctOptionId = UUID.randomUUID();
        UUID otherOptionId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        Instant validFrom = now.minusSeconds(60);
        Instant validTo = now.plusSeconds(60);
        QuizAuthoring.Draft draft = new QuizAuthoring.Draft(
            "  Mapped quiz  ",
            "  Description  ",
            subjectId,
            "  Chapter  ",
            QuizStatus.DRAFT,
            1,
            true,
            validFrom,
            validTo,
            null,
            List.of(new QuizAuthoring.QuestionDraft(
                questionId,
                QuestionType.MULTIPLE_CHOICE,
                "  Prompt  ",
                3,
                "  code();  ",
                mediaId,
                45,
                List.of(
                    new QuizAuthoring.OptionDraft(
                        correctOptionId,
                        "  Correct  ",
                        true
                    ),
                    new QuizAuthoring.OptionDraft(
                        otherOptionId,
                        "  Other  ",
                        false
                    )
                ),
                List.of()
            ))
        );

        QuizAuthoring.Quiz quiz = service.create(teacherId, draft);

        assertThat(quiz.creatorId()).isEqualTo(teacherId);
        assertThat(quiz.subjectName()).isEqualTo("Programming");
        assertThat(quiz.title()).isEqualTo("Mapped quiz");
        assertThat(quiz.description()).isEqualTo("Description");
        assertThat(quiz.chapter()).isEqualTo("Chapter");
        assertThat(quiz.shuffle()).isTrue();
        assertThat(quiz.validFrom()).isEqualTo(validFrom);
        assertThat(quiz.validTo()).isEqualTo(validTo);
        assertThat(quiz.questions()).singleElement().satisfies(question -> {
            assertThat(question.id()).isEqualTo(questionId);
            assertThat(question.prompt()).isEqualTo("Prompt");
            assertThat(question.codeSnippet()).isEqualTo("code();");
            assertThat(question.mediaId()).isEqualTo(mediaId);
            assertThat(question.points()).isEqualTo(3);
            assertThat(question.timeSeconds()).isEqualTo(45);
            assertThat(question.options())
                .extracting(QuizAuthoring.Option::id)
                .containsExactly(correctOptionId, otherOptionId);
            assertThat(question.options())
                .extracting(QuizAuthoring.Option::text)
                .containsExactly("Correct", "Other");
        });
        verify(mediaUsagePolicy).requireUsable(mediaId, teacherId, false);
    }

    @Test
    void hidesQuizFromAnotherTeacher() {
        QuizResponse created = service.createForTeacher(
            teacherId,
            validRequest(QuizStatus.DRAFT)
        );
        when(repository.findById(created.id())).thenReturn(Optional.of(saved.get()));

        assertThatThrownBy(() -> service.getForTeacher(UUID.randomUUID(), created.id()))
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("quiz_not_found")
            );
    }

    @Test
    void rejectsStaleReplacementVersion() {
        QuizResponse created = service.createForTeacher(
            teacherId,
            validRequest(QuizStatus.DRAFT)
        );
        when(repository.findById(created.id())).thenReturn(Optional.of(saved.get()));
        QuizWriteRequest stale = withExpectedVersion(validRequest(QuizStatus.DRAFT), 7L);

        assertThatThrownBy(() -> service.replaceForTeacher(teacherId, created.id(), stale))
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("quiz_conflict")
            );
    }

    @Test
    void rejectsMultipleChoiceWithoutCorrectAnswer() {
        QuestionWriteRequest invalidQuestion = new QuestionWriteRequest(
            null,
            QuestionType.MULTIPLE_CHOICE,
            "Pick one",
            1,
            null,
            null,
            30,
            List.of(
                new OptionWriteRequest(null, "A", false),
                new OptionWriteRequest(null, "B", false)
            ),
            List.of()
        );
        QuizWriteRequest request = new QuizWriteRequest(
            "Broken",
            null,
            subjectId,
            null,
            QuizStatus.DRAFT,
            1,
            false,
            null,
            null,
            null,
            List.of(invalidQuestion)
        );

        assertThatThrownBy(() -> service.createForTeacher(teacherId, request))
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("quiz_invalid")
            );
    }

    @Test
    void validatesDirectServiceCallsWithoutMvc() {
        QuizWriteRequest valid = validRequest(QuizStatus.DRAFT);
        QuizWriteRequest invalid = new QuizWriteRequest(
            " ",
            valid.description(),
            valid.subjectId(),
            valid.chapter(),
            valid.status(),
            valid.maxQuestionsPerSession(),
            valid.shuffle(),
            valid.validFrom(),
            valid.validTo(),
            valid.expectedVersion(),
            valid.questions()
        );

        assertThatThrownBy(() -> service.createForTeacher(teacherId, invalid))
            .isInstanceOfSatisfying(ProblemException.class, exception -> {
                assertThat(exception.code()).isEqualTo("quiz_invalid");
                assertThat(exception.getMessage()).contains("title");
            });
    }

    @Test
    void checksQuestionMediaOnlyOnce() {
        UUID mediaId = UUID.randomUUID();
        QuizWriteRequest valid = validRequest(QuizStatus.DRAFT);
        QuestionWriteRequest original = valid.questions().get(0);
        QuestionWriteRequest withMedia = new QuestionWriteRequest(
            original.id(),
            original.type(),
            original.prompt(),
            original.points(),
            original.codeSnippet(),
            mediaId,
            original.timeSeconds(),
            original.options(),
            original.pairs()
        );
        QuizWriteRequest request = new QuizWriteRequest(
            valid.title(),
            valid.description(),
            valid.subjectId(),
            valid.chapter(),
            valid.status(),
            valid.maxQuestionsPerSession(),
            valid.shuffle(),
            valid.validFrom(),
            valid.validTo(),
            valid.expectedVersion(),
            List.of(withMedia)
        );

        service.createForTeacher(teacherId, request);

        verify(mediaUsagePolicy).requireUsable(mediaId, teacherId, false);
    }

    private QuizWriteRequest validRequest(QuizStatus status) {
        QuestionWriteRequest question = new QuestionWriteRequest(
            null,
            QuestionType.MULTIPLE_CHOICE,
            "Which protocol is secure?",
            2,
            null,
            null,
            30,
            List.of(
                new OptionWriteRequest(null, "HTTPS", true),
                new OptionWriteRequest(null, "HTTP", false)
            ),
            List.of()
        );
        return new QuizWriteRequest(
            "Network basics",
            "Revision",
            subjectId,
            "Protocols",
            status,
            1,
            false,
            null,
            null,
            null,
            List.of(question)
        );
    }

    private QuizWriteRequest withExpectedVersion(QuizWriteRequest source, long version) {
        return new QuizWriteRequest(
            source.title(),
            source.description(),
            source.subjectId(),
            source.chapter(),
            source.status(),
            source.maxQuestionsPerSession(),
            source.shuffle(),
            source.validFrom(),
            source.validTo(),
            version,
            source.questions()
        );
    }
}
