package com.sosehl.curtis_backend.domain.v1.question.dto;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.MatchingPair;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuestionResponse {

    private String question;
    private QuestionType type;
    private Integer points;
    private String codeSnippet;
    private String imageRef;
    private List<QuestionAnswer> answers;
    private List<MatchingPair> pairs;
    private Integer timeInSeconds;
    private UUID quizUuid;
}
