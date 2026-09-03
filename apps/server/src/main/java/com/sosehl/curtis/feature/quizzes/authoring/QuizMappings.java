package com.sosehl.curtis.feature.quizzes.authoring;

import com.sosehl.curtis.feature.quizzes.MatchingPairEntity;
import com.sosehl.curtis.feature.quizzes.QuestionEntity;
import com.sosehl.curtis.feature.quizzes.QuestionOptionEntity;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.OptionResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.OptionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.PairResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.PairWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuestionResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuestionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizWriteRequest;
import com.sosehl.curtis.feature.quizzes.QuizEntity;
import com.sosehl.curtis.feature.quizzes.QuizErrors;
import com.sosehl.curtis.feature.quizzes.core.OptionSnapshot;
import com.sosehl.curtis.feature.quizzes.core.PairSnapshot;
import com.sosehl.curtis.feature.quizzes.core.QuestionSnapshot;
import com.sosehl.curtis.feature.quizzes.core.QuizAuthoring;
import com.sosehl.curtis.feature.quizzes.core.QuizSnapshot;
import java.util.List;

public final class QuizMappings {

    private QuizMappings() {}

    public static QuizResponse toResponse(QuizEntity quiz, String subjectName) {
        return new QuizResponse(
            quiz.getId(),
            quiz.getVersion(),
            quiz.getCreatorId(),
            quiz.getSubjectId(),
            subjectName,
            quiz.getTitle(),
            quiz.getDescription(),
            quiz.getChapter(),
            quiz.getStatus(),
            quiz.getMaxQuestionsPerSession(),
            quiz.isShuffle(),
            quiz.getValidFrom(),
            quiz.getValidTo(),
            quiz.getCreatedAt(),
            quiz.getUpdatedAt(),
            quiz.getArchivedAt(),
            quiz.getQuestions().stream().map(QuizMappings::toResponse).toList()
        );
    }

    public static QuizSnapshot toSnapshot(QuizEntity quiz, String subjectName) {
        return new QuizSnapshot(
            quiz.getId(),
            quiz.getVersion(),
            quiz.getTitle(),
            quiz.getDescription(),
            subjectName,
            quiz.getChapter(),
            quiz.getMaxQuestionsPerSession(),
            quiz.isShuffle(),
            quiz.getQuestions().stream().map(QuizMappings::toSnapshot).toList()
        );
    }

    public static QuizWriteRequest toWriteRequest(QuizAuthoring.Draft draft) {
        if (draft == null) {
            throw QuizErrors.invalid("quiz is required");
        }
        List<QuestionWriteRequest> questions = draft.questions() == null
            ? null
            : draft.questions().stream()
                .map(QuizMappings::toWriteRequest)
                .toList();
        return new QuizWriteRequest(
            draft.title(),
            draft.description(),
            draft.subjectId(),
            draft.chapter(),
            draft.status(),
            draft.maxQuestionsPerSession(),
            draft.shuffle(),
            draft.validFrom(),
            draft.validTo(),
            draft.expectedVersion(),
            questions
        );
    }

    public static QuizAuthoring.Quiz toAuthoringQuiz(QuizResponse quiz) {
        return new QuizAuthoring.Quiz(
            quiz.id(),
            quiz.version(),
            quiz.creatorId(),
            quiz.subjectId(),
            quiz.subjectName(),
            quiz.title(),
            quiz.description(),
            quiz.chapter(),
            quiz.status(),
            quiz.maxQuestionsPerSession(),
            quiz.shuffle(),
            quiz.validFrom(),
            quiz.validTo(),
            quiz.createdAt(),
            quiz.updatedAt(),
            quiz.archivedAt(),
            quiz.questions().stream()
                .map(QuizMappings::toAuthoringQuestion)
                .toList()
        );
    }

    private static QuestionResponse toResponse(QuestionEntity question) {
        return new QuestionResponse(
            question.getId(),
            question.getPosition(),
            question.getType(),
            question.getPrompt(),
            question.getPoints(),
            question.getCodeSnippet(),
            question.getMediaId(),
            question.getTimeSeconds(),
            question.getOptions().stream().map(QuizMappings::toResponse).toList(),
            question.getPairs().stream().map(QuizMappings::toResponse).toList()
        );
    }

    private static OptionResponse toResponse(QuestionOptionEntity option) {
        return new OptionResponse(
            option.getId(),
            option.getPosition(),
            option.getText(),
            option.isCorrect()
        );
    }

    private static PairResponse toResponse(MatchingPairEntity pair) {
        return new PairResponse(
            pair.getId(),
            pair.getPosition(),
            pair.getLeftText(),
            pair.getRightText()
        );
    }

    private static QuestionSnapshot toSnapshot(QuestionEntity question) {
        return new QuestionSnapshot(
            question.getId(),
            question.getPosition(),
            question.getType(),
            question.getPrompt(),
            question.getPoints(),
            question.getCodeSnippet(),
            question.getMediaId(),
            question.getTimeSeconds(),
            question.getOptions().stream().map(QuizMappings::toSnapshot).toList(),
            question.getPairs().stream().map(QuizMappings::toSnapshot).toList()
        );
    }

    private static OptionSnapshot toSnapshot(QuestionOptionEntity option) {
        return new OptionSnapshot(
            option.getId(),
            option.getPosition(),
            option.getText(),
            option.isCorrect()
        );
    }

    private static PairSnapshot toSnapshot(MatchingPairEntity pair) {
        return new PairSnapshot(
            pair.getId(),
            pair.getPosition(),
            pair.getLeftText(),
            pair.getRightText()
        );
    }

    private static QuestionWriteRequest toWriteRequest(
        QuizAuthoring.QuestionDraft question
    ) {
        if (question == null) return null;
        List<OptionWriteRequest> options = question.options() == null
            ? null
            : question.options().stream()
                .map(QuizMappings::toWriteRequest)
                .toList();
        List<PairWriteRequest> pairs = question.pairs() == null
            ? null
            : question.pairs().stream()
                .map(QuizMappings::toWriteRequest)
                .toList();
        return new QuestionWriteRequest(
            question.id(),
            question.type(),
            question.prompt(),
            question.points(),
            question.codeSnippet(),
            question.mediaId(),
            question.timeSeconds(),
            options,
            pairs
        );
    }

    private static OptionWriteRequest toWriteRequest(
        QuizAuthoring.OptionDraft option
    ) {
        if (option == null) return null;
        return new OptionWriteRequest(option.id(), option.text(), option.correct());
    }

    private static PairWriteRequest toWriteRequest(QuizAuthoring.PairDraft pair) {
        if (pair == null) return null;
        return new PairWriteRequest(pair.id(), pair.left(), pair.right());
    }

    private static QuizAuthoring.Question toAuthoringQuestion(
        QuestionResponse question
    ) {
        return new QuizAuthoring.Question(
            question.id(),
            question.position(),
            question.type(),
            question.prompt(),
            question.points(),
            question.codeSnippet(),
            question.mediaId(),
            question.timeSeconds(),
            question.options().stream()
                .map(option -> new QuizAuthoring.Option(
                    option.id(),
                    option.position(),
                    option.text(),
                    option.correct()
                ))
                .toList(),
            question.pairs().stream()
                .map(pair -> new QuizAuthoring.Pair(
                    pair.id(),
                    pair.position(),
                    pair.left(),
                    pair.right()
                ))
                .toList()
        );
    }
}
