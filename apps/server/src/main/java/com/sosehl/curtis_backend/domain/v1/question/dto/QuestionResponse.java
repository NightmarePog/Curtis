package com.sosehl.curtis_backend.domain.v1.question.dto;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuestionResponse {

    private String question;
    private List<QuestionAnswer> answers;
    private Integer timeInSeconds;
    private UUID quizUuid;
}
