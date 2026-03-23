package com.sosehl.curtis_backend.mappers;

import com.sosehl.curtis_backend.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.models.QuestionsModel;
import com.sosehl.curtis_backend.models.QuizModel;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QuizMapper {

    public QuizModel mapCreateQuiz(QuizCreateRequest quizCreateDto) {
        QuizModel quizModel = new QuizModel();

        List<QuestionsModel> mappedQuestions = quizCreateDto
            .getQuestions()
            .stream()
            .map(mapper -> {
                QuestionsModel question = new QuestionsModel();
                question.setQuestion(mapper.getQuestion());
                question.setAnswers(mapper.getPossibleAnswers());
                question.setQuiz(quizModel);
                question.setCorrectAnswers(mapper.getAnswersId());
                return question;
            })
            .toList();

        quizModel.setTitle(quizCreateDto.getTitle());
        quizModel.setDescription(quizCreateDto.getDescription());
        quizModel.setQuestions(mappedQuestions);

        return quizModel;
    }
}
