package com.sosehl.curtis.feature.quizzes.authoring;

import com.sosehl.curtis.feature.media.core.MediaUsagePolicy;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.OptionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.PairWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuestionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizWriteRequest;
import com.sosehl.curtis.feature.quizzes.QuizErrors;
import com.sosehl.curtis.feature.quizzes.core.QuizStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class QuizRequestValidator {

    private final Validator validator;
    private final MediaUsagePolicy mediaUsagePolicy;

    public QuizRequestValidator(
        Validator validator,
        MediaUsagePolicy mediaUsagePolicy
    ) {
        this.validator = validator;
        this.mediaUsagePolicy = mediaUsagePolicy;
    }

    public void requireStructurallyValid(QuizWriteRequest request) {
        if (request == null) {
            throw QuizErrors.invalid("quiz is required");
        }
        ConstraintViolation<QuizWriteRequest> violation = validator
            .validate(request)
            .stream()
            .min(Comparator.comparing(value -> value.getPropertyPath().toString()))
            .orElse(null);
        if (violation != null) {
            String path = violation.getPropertyPath().toString();
            String prefix = path.isBlank() ? "" : path + " ";
            throw QuizErrors.invalid(prefix + violation.getMessage());
        }
    }

    public void validateRules(
        QuizWriteRequest request,
        UUID mediaOwnerId,
        boolean administrator
    ) {
        if (
            request.validFrom() != null &&
            request.validTo() != null &&
            !request.validTo().isAfter(request.validFrom())
        ) {
            throw QuizErrors.invalid("validTo must be after validFrom");
        }
        if (request.status() == QuizStatus.ARCHIVED) {
            throw QuizErrors.invalid("Use the archive operation to archive a quiz");
        }
        if (
            request.status() == QuizStatus.PUBLISHED &&
            request.questions().isEmpty()
        ) {
            throw QuizErrors.invalid("A published quiz must contain a question");
        }
        if (
            !request.questions().isEmpty() &&
            request.maxQuestionsPerSession() > request.questions().size()
        ) {
            throw QuizErrors.invalid(
                "maxQuestionsPerSession cannot exceed the question count"
            );
        }

        Set<UUID> ids = new HashSet<>();
        for (int index = 0; index < request.questions().size(); index++) {
            QuestionWriteRequest question = request.questions().get(index);
            addUnique(ids, question.id(), "question");
            for (OptionWriteRequest option : question.options()) {
                addUnique(ids, option.id(), "option");
            }
            for (PairWriteRequest pair : question.pairs()) {
                addUnique(ids, pair.id(), "matching pair");
            }
            validateQuestion(index, question);
            if (question.mediaId() != null) {
                mediaUsagePolicy.requireUsable(
                    question.mediaId(),
                    mediaOwnerId,
                    administrator
                );
            }
        }
    }

    private void validateQuestion(int index, QuestionWriteRequest question) {
        boolean hasOptions = !question.options().isEmpty();
        boolean hasPairs = !question.pairs().isEmpty();
        switch (question.type()) {
            case MULTIPLE_CHOICE -> {
                if (
                    question.options().size() < 2 ||
                    hasPairs ||
                    question.options().stream().noneMatch(OptionWriteRequest::correct)
                ) {
                    throw invalidQuestion(
                        index,
                        "requires at least two options and a correct answer"
                    );
                }
            }
            case MATCHING -> {
                if (question.pairs().size() < 2 || hasOptions) {
                    throw invalidQuestion(
                        index,
                        "requires at least two pairs and no options"
                    );
                }
            }
            case FREE_TEXT -> {
                if (hasOptions || hasPairs) {
                    throw invalidQuestion(index, "cannot contain options or pairs");
                }
            }
        }
    }

    private RuntimeException invalidQuestion(int index, String detail) {
        return QuizErrors.invalid("Question " + (index + 1) + " " + detail);
    }

    private void addUnique(Set<UUID> ids, UUID id, String kind) {
        if (id != null && !ids.add(id)) {
            throw QuizErrors.invalid("Duplicate " + kind + " id");
        }
    }
}
