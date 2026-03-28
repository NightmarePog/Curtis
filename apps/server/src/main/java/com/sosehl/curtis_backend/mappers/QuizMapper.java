package com.sosehl.curtis_backend.mappers;

import com.sosehl.curtis_backend.dto.receive.QuestionCreateDto;
import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.receive.QuizPatchRequest;
import com.sosehl.curtis_backend.dto.response.QuestionResponse;
import com.sosehl.curtis_backend.dto.response.QuizGetResponse;
import com.sosehl.curtis_backend.models.QuizModel;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QuizMapper {

    public QuizModel mapCreateQuiz(QuizCreateRequest quizCreateDto) {
        QuizModel quizModel = new QuizModel();

        quizModel.setTitle(quizCreateDto.getTitle());
        quizModel.setDescription(quizCreateDto.getDescription());

        return quizModel;
    }

    public QuizGetResponse mapGetResponse(QuizModel model) {
        QuizGetResponse response = new QuizGetResponse();

        List<QuestionResponse> questions = model
            .getQuestions()
            .stream()
            .map(q -> {
                QuestionResponse r = new QuestionResponse();
                r.setQuestionOrder(q.getQuestionOrder());
                r.setQuestion(q.getQuestion());
                r.setAnswers(q.getAnswers());
                r.setCorrectAnswers(q.getCorrectAnswers());
                r.setTime(q.getTime());
                return r;
            })
            .toList();

        response.setUuid(model.getUuid());
        response.setTitle(model.getTitle());
        response.setDescription(model.getDescription());
        response.setQuestions(questions);

        return response;
    }

    public QuizModel mapPatchQuiz(
        QuizPatchRequest request,
        QuizModel existingQuiz
    ) {
        if (request.getTitle() != null) {
            existingQuiz.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            existingQuiz.setDescription(request.getDescription());
        }

        return existingQuiz;
    }
}
