package com.sosehl.curtis_backend.mappers;

import com.sosehl.curtis_backend.dto.receive.QuestionCreateDto;
import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.receive.QuizPatchRequest;
import com.sosehl.curtis_backend.dto.response.QuizGetResponse;
import com.sosehl.curtis_backend.models.QuizModel;
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

        response.setUuid(model.getUuid());
        response.setTitle(model.getTitle());
        response.setQuestions(
            model
                .getQuestions()
                .stream()
                .map(mapper -> {
                    QuestionCreateDto dto = new QuestionCreateDto();
                    dto.setQuestion(mapper.getQuestion());
                    dto.setTime(mapper.getTime());
                    return dto;
                })
                .toList()
        );
        response.setDescription(model.getDescription());
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
