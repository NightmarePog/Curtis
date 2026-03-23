package com.sosehl.curtis_backend.mappers;

import com.sosehl.curtis_backend.dto.quiz.QuestionDto;
import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.response.QuizGetResponse;
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
                question.setTime(mapper.getTime());
                return question;
            })
            .toList();

        quizModel.setTitle(quizCreateDto.getTitle());
        quizModel.setDescription(quizCreateDto.getDescription());
        quizModel.setQuestions(mappedQuestions);

        return quizModel;
    }

    public QuizGetResponse mapGetResponse(QuizModel model) {
        QuizGetResponse response = new QuizGetResponse();

        response.setUuid(model.getUuid());
        response.setTitle(model.getTitle());
        response.setQuestions(
            model
                .getQuestions()
                .stream()
                .map(mapper -> {
                    QuestionDto dto = new QuestionDto();
                    dto.setQuestion(mapper.getQuestion());
                    dto.setPossibleAnswers(mapper.getAnswers());
                    dto.setAnswersId(mapper.getCorrectAnswers());
                    dto.setTime(mapper.getTime());
                    return dto;
                })
                .toList()
        );
        response.setDescription(model.getDescription());
        return response;
    }
}
