package com.sosehl.curtis_backend.domain.v1.quiz;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
import java.util.List;

public class QuizMapper {

    public static Quiz mapCreateQuiz(QuizCreateRequest quizCreateDto) {
        Quiz quizModel = new Quiz();

        quizModel.setTitle(quizCreateDto.getTitle());
        quizModel.setDescription(quizCreateDto.getDescription());

        return quizModel;
    }

    public static QuizGetResponse mapGetResponse(Quiz model) {
        QuizGetResponse response = new QuizGetResponse();

        List<QuestionResponse> questions = model
            .getQuestions()
            .stream()
            .map(q -> {
                QuestionResponse r = new QuestionResponse();
                r.setQuestion(q.getQuestion());

                return r;
            })
            .toList();

        response.setUuid(model.getUuid());
        response.setTitle(model.getTitle());
        response.setDescription(model.getDescription());
        response.setQuestions(questions);

        return response;
    }

    public static Quiz mapPatchQuiz(
        QuizPatchRequest request,
        Quiz existingQuiz
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
