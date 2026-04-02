package com.sosehl.curtis_backend.domain.v1.question;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionCreateDto;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionPatchDto;
import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

    public Question createMap(QuestionCreateDto qDto, Quiz quizModel) {
        Question model = new Question();

        model.setQuiz(quizModel);
        model.setTimeInSeconds(qDto.getTimeInSeconds());
        model.setQuestion(qDto.getQuestion());
        model.setAnswers(qDto.getAnswers());

        return model;
    }

    public Question patchMap(QuestionPatchDto qDto, Question model) {
        if (qDto.getTimeInSeconds() != null) {
            model.setTimeInSeconds(qDto.getTimeInSeconds());
        }

        if (qDto.getQuestion() != null) {
            model.setQuestion(qDto.getQuestion());
        }

        if (qDto.getAnswers() != null) {
            model.setAnswers(qDto.getAnswers());
        }

        return model;
    }
}
