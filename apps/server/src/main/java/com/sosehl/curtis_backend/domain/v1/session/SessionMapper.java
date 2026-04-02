package com.sosehl.curtis_backend.domain.v1.session;

import com.sosehl.curtis_backend.domain.v1.question.Question;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;

public class SessionMapper {

    public static QuestionResponse toQuestionResponse(Question questionModel) {
        QuestionResponse questionResponse = new QuestionResponse();
        questionResponse.setAnswers(questionModel.getAnswers());
        questionResponse.setCorrectAnswers(questionModel.getCorrectAnswers());
        questionResponse.setQuestionOrder(questionModel.getQuestionOrder());
        questionResponse.setQuestion(questionModel.getQuestion());
        questionResponse.setTime(questionModel.getTime());
        return questionResponse;
    }
}
