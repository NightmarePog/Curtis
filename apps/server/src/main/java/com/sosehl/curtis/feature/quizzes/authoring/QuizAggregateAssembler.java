package com.sosehl.curtis.feature.quizzes.authoring;

import com.sosehl.curtis.feature.quizzes.MatchingPairEntity;
import com.sosehl.curtis.feature.quizzes.QuestionEntity;
import com.sosehl.curtis.feature.quizzes.QuestionOptionEntity;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.OptionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.PairWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuestionWriteRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizWriteRequest;
import com.sosehl.curtis.feature.quizzes.QuizEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class QuizAggregateAssembler {

    private QuizAggregateAssembler() {}

    public static QuizEntity create(
        UUID creatorId,
        QuizWriteRequest request,
        Instant now
    ) {
        QuizEntity quiz = new QuizEntity(UUID.randomUUID(), creatorId, now);
        apply(quiz, request, now);
        return quiz;
    }

    public static void apply(
        QuizEntity quiz,
        QuizWriteRequest request,
        Instant now
    ) {
        quiz.setTitle(request.title().trim());
        quiz.setDescription(clean(request.description()));
        quiz.setSubjectId(request.subjectId());
        quiz.setChapter(clean(request.chapter()));
        quiz.setStatus(request.status());
        quiz.setMaxQuestionsPerSession(request.maxQuestionsPerSession());
        quiz.setShuffle(request.shuffle());
        quiz.setValidFrom(request.validFrom());
        quiz.setValidTo(request.validTo());
        quiz.setUpdatedAt(now);

        List<QuestionEntity> questions = new ArrayList<>();
        for (int position = 0; position < request.questions().size(); position++) {
            questions.add(toQuestion(request.questions().get(position), position, now));
        }
        quiz.replaceQuestions(questions);
    }

    private static QuestionEntity toQuestion(
        QuestionWriteRequest value,
        int position,
        Instant now
    ) {
        QuestionEntity question = new QuestionEntity(
            idOrNew(value.id()),
            position,
            now
        );
        question.setType(value.type());
        question.setPrompt(value.prompt().trim());
        question.setPoints(value.points());
        question.setCodeSnippet(clean(value.codeSnippet()));
        question.setMediaId(value.mediaId());
        question.setTimeSeconds(value.timeSeconds());

        List<QuestionOptionEntity> options = new ArrayList<>();
        for (
            int optionPosition = 0;
            optionPosition < value.options().size();
            optionPosition++
        ) {
            options.add(
                toOption(value.options().get(optionPosition), optionPosition)
            );
        }
        question.replaceOptions(options);

        List<MatchingPairEntity> pairs = new ArrayList<>();
        for (
            int pairPosition = 0;
            pairPosition < value.pairs().size();
            pairPosition++
        ) {
            pairs.add(toPair(value.pairs().get(pairPosition), pairPosition));
        }
        question.replacePairs(pairs);
        return question;
    }

    private static QuestionOptionEntity toOption(
        OptionWriteRequest value,
        int position
    ) {
        return new QuestionOptionEntity(
            idOrNew(value.id()),
            position,
            value.text().trim(),
            value.correct()
        );
    }

    private static MatchingPairEntity toPair(PairWriteRequest value, int position) {
        return new MatchingPairEntity(
            idOrNew(value.id()),
            position,
            value.left().trim(),
            value.right().trim()
        );
    }

    private static UUID idOrNew(UUID id) {
        return id == null ? UUID.randomUUID() : id;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
