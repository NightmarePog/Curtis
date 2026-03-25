package com.sosehl.curtis_backend.mappers;

import com.sosehl.curtis_backend.dto.receive.QuestionCreateDto;
import com.sosehl.curtis_backend.dto.receive.QuestionPatchDto;
import com.sosehl.curtis_backend.models.QuestionModel;
import com.sosehl.curtis_backend.models.QuizModel;

public class QuestionMapper {

    public QuestionModel createMap(
        QuestionCreateDto qDto,
        QuizModel quizModel
    ) {
        QuestionModel model = new QuestionModel();

        model.setQuiz(quizModel);
        model.setTime(qDto.getTime());
        model.setQuestion(qDto.getQuestion());
        model.setCorrectAnswers(qDto.getCorrectAnswers());
        model.setAnswers(qDto.getAnswers());
        model.setOrder(qDto.getOrder());

        return model;
    }

    public QuestionModel patchMap(QuestionPatchDto qDto, QuestionModel model) {
        if (qDto.getTime() != null) {
            model.setTime(qDto.getTime());
        }

        if (qDto.getQuestion() != null) {
            model.setQuestion(qDto.getQuestion());
        }

        if (qDto.getCorrectAnswers() != null) {
            model.setCorrectAnswers(qDto.getCorrectAnswers());
        }

        if (qDto.getAnswer() != null) {
            model.setAnswers(qDto.getAnswer());
        }

        return model;
    }
}
