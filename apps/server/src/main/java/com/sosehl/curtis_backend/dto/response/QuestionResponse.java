package com.sosehl.curtis_backend.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class QuestionResponse {

    private Integer questionOrder;
    private String question;
    private List<String> answers;
    private List<Integer> correctAnswers;
    private Integer time;
}
