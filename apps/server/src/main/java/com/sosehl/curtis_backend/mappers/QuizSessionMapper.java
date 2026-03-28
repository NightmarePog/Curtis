package com.sosehl.curtis_backend.mappers;

import com.sosehl.curtis_backend.dto.response.QuestionResponse;
import com.sosehl.curtis_backend.models.QuestionModel;
import com.sosehl.curtis_backend.models.QuizSessionModel;
import java.util.List;
import java.util.UUID;

public class QuizSessionMapper {

    public static QuizSessionModel toSession(
        String userId,
        List<QuestionModel> questions
    ) {
        QuizSessionModel session = new QuizSessionModel();
        session.setSessionUUID(UUID.randomUUID());
        session.setSessionOwnerID(userId);
        session.setQuestionCount(questions.size());
        session.setQuestions(questions);
        session.setCurrentQuestionIndex(1);
        return session;
    }

    public static QuestionResponse toQuestionResponse(
        QuestionModel questionModel
    ) {
        QuestionResponse questionResponse = new QuestionResponse();
        questionResponse.setAnswers(questionModel.getAnswers());
        questionResponse.setCorrectAnswers(questionModel.getCorrectAnswers());
        questionResponse.setQuestionOrder(questionModel.getQuestionOrder());
        questionResponse.setQuestion(questionModel.getQuestion());
        questionResponse.setTime(questionModel.getTime());
        return questionResponse;
    }
}
