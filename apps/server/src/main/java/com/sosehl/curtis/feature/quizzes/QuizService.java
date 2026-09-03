package com.sosehl.curtis.feature.quizzes;

import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizWriteRequest;
import com.sosehl.curtis.feature.quizzes.authoring.QuizAggregateAssembler;
import com.sosehl.curtis.feature.quizzes.authoring.QuizMappings;
import com.sosehl.curtis.feature.quizzes.authoring.QuizRequestValidator;
import com.sosehl.curtis.feature.quizzes.core.QuizAuthoring;
import com.sosehl.curtis.feature.quizzes.core.QuizChangeNotifier;
import com.sosehl.curtis.feature.quizzes.core.QuizSnapshot;
import com.sosehl.curtis.feature.quizzes.core.QuizSnapshotProvider;
import com.sosehl.curtis.feature.quizzes.core.QuizStatus;
import com.sosehl.curtis.feature.subjects.core.SubjectCatalog;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SOSE_ReadOnlyTransaction
public class QuizService implements QuizSnapshotProvider, QuizAuthoring {

    private final QuizRepository quizzes;
    private final SubjectCatalog subjects;
    private final Clock clock;
    private final QuizChangeNotifier changes;
    private final QuizRequestValidator requestValidator;

    QuizService(
        QuizRepository quizzes,
        SubjectCatalog subjects,
        Clock clock,
        QuizChangeNotifier changes,
        QuizRequestValidator requestValidator
    ) {
        this.quizzes = quizzes;
        this.subjects = subjects;
        this.clock = clock;
        this.changes = changes;
        this.requestValidator = requestValidator;
    }

    @Override
    @Transactional
    public QuizAuthoring.Quiz create(UUID teacherId, QuizAuthoring.Draft draft) {
        return QuizMappings.toAuthoringQuiz(
            createForTeacher(teacherId, QuizMappings.toWriteRequest(draft))
        );
    }

    @Override
    @Transactional
    public QuizAuthoring.Quiz replace(
        UUID teacherId,
        UUID quizId,
        QuizAuthoring.Draft draft
    ) {
        return QuizMappings.toAuthoringQuiz(
            replaceForTeacher(
                teacherId,
                quizId,
                QuizMappings.toWriteRequest(draft)
            )
        );
    }

    @Override
    public QuizAuthoring.Quiz get(UUID teacherId, UUID quizId) {
        return QuizMappings.toAuthoringQuiz(getForTeacher(teacherId, quizId));
    }

    @Transactional
    public QuizResponse createForTeacher(UUID teacherId, QuizWriteRequest request) {
        return create(teacherId, request, false);
    }

    @Transactional
    public QuizResponse createByAdministrator(
        UUID creatorId,
        QuizWriteRequest request
    ) {
        return create(creatorId, request, true);
    }

    public List<QuizResponse> listForTeacher(UUID teacherId) {
        return quizzes.findAllByCreatorIdOrderByUpdatedAtDesc(teacherId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<QuizResponse> listForAdministrator() {
        return quizzes.findAllByOrderByUpdatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public QuizResponse getForTeacher(UUID teacherId, UUID quizId) {
        return toResponse(requireOwned(teacherId, quizId));
    }

    public QuizResponse getForAdministrator(UUID quizId) {
        return toResponse(requireAny(quizId));
    }

    @Transactional
    public QuizResponse replaceForTeacher(
        UUID teacherId,
        UUID quizId,
        QuizWriteRequest request
    ) {
        QuizEntity quiz = requireOwned(teacherId, quizId);
        apply(quiz, request, teacherId, false);
        QuizResponse updated = toResponse(quizzes.saveAndFlush(quiz));
        quizChanged();
        return updated;
    }

    @Transactional
    public QuizResponse replaceByAdministrator(
        UUID quizId,
        QuizWriteRequest request
    ) {
        QuizEntity quiz = requireAny(quizId);
        apply(quiz, request, quiz.getCreatorId(), true);
        QuizResponse updated = toResponse(quizzes.saveAndFlush(quiz));
        quizChanged();
        return updated;
    }

    @Transactional
    public void archiveForTeacher(UUID teacherId, UUID quizId, long expectedVersion) {
        QuizEntity quiz = requireOwned(teacherId, quizId);
        requireVersion(quiz, expectedVersion);
        quiz.archive(clock.instant());
        quizChanged();
    }

    @Transactional
    public void archiveByAdministrator(
        UUID quizId,
        long expectedVersion
    ) {
        QuizEntity quiz = requireAny(quizId);
        requireVersion(quiz, expectedVersion);
        quiz.archive(clock.instant());
        quizChanged();
    }

    @Override
    public QuizSnapshot loadLaunchable(UUID quizId, UUID teacherUserId) {
        QuizEntity quiz = requireOwned(teacherUserId, quizId);
        Instant now = clock.instant();
        if (
            quiz.getStatus() != QuizStatus.PUBLISHED ||
            quiz.getQuestions().isEmpty() ||
            (quiz.getValidFrom() != null && now.isBefore(quiz.getValidFrom())) ||
            (quiz.getValidTo() != null && !now.isBefore(quiz.getValidTo()))
        ) {
            throw QuizErrors.conflict("Quiz is not launchable");
        }
        subjects.requireTeacherAssignment(teacherUserId, quiz.getSubjectId());
        return toSnapshot(quiz);
    }

    private QuizResponse create(
        UUID creatorId,
        QuizWriteRequest request,
        boolean administrator
    ) {
        requestValidator.requireStructurallyValid(request);
        if (!administrator) {
            subjects.requireTeacherAssignment(creatorId, request.subjectId());
        } else {
            // An administrator may repair an unassigned subject, but launch still
            // requires the creator to have the assignment at that time.
            subjects.require(request.subjectId());
        }
        requestValidator.validateRules(request, creatorId, administrator);
        Instant now = clock.instant();
        QuizEntity quiz = QuizAggregateAssembler.create(creatorId, request, now);
        QuizResponse response = toResponse(quizzes.saveAndFlush(quiz));
        quizChanged();
        return response;
    }

    private void apply(
        QuizEntity quiz,
        QuizWriteRequest request,
        UUID mediaOwnerId,
        boolean administrator
    ) {
        requestValidator.requireStructurallyValid(request);
        if (request.expectedVersion() == null) {
            throw QuizErrors.invalid("expectedVersion is required");
        }
        requireVersion(quiz, request.expectedVersion());
        if (!administrator) {
            subjects.requireTeacherAssignment(mediaOwnerId, request.subjectId());
        } else {
            subjects.require(request.subjectId());
        }
        requestValidator.validateRules(request, mediaOwnerId, administrator);
        QuizAggregateAssembler.apply(quiz, request, clock.instant());
    }

    private void requireVersion(QuizEntity quiz, long expected) {
        if (quiz.getVersion() != expected) {
            throw QuizErrors.conflict("Quiz was modified by another request");
        }
    }

    private QuizEntity requireOwned(UUID teacherId, UUID quizId) {
        QuizEntity quiz = requireAny(quizId);
        if (!quiz.getCreatorId().equals(teacherId)) {
            throw QuizErrors.notFound();
        }
        return quiz;
    }

    private QuizEntity requireAny(UUID quizId) {
        return quizzes.findById(quizId).orElseThrow(QuizErrors::notFound);
    }

    private QuizResponse toResponse(QuizEntity quiz) {
        return QuizMappings.toResponse(
            quiz,
            subjects.require(quiz.getSubjectId()).name()
        );
    }

    private QuizSnapshot toSnapshot(QuizEntity quiz) {
        return QuizMappings.toSnapshot(
            quiz,
            subjects.require(quiz.getSubjectId()).name()
        );
    }

    private void quizChanged() {
        changes.quizzesChanged();
    }
}
