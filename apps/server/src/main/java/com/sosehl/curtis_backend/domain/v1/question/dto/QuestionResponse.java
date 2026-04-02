package com.sosehl.curtis_backend.domain.v1.question.dto;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuestionResponse {

    private Integer questionOrder;
    private String question;
    private List<String> answers;
    private List<Integer> correctAnswers;
    private Integer time;
    private UUID sessionUuid;
}
