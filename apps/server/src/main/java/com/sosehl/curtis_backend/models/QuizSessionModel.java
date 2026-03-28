package com.sosehl.curtis_backend.models;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuizSessionModel {

    private UUID sessionUUID;
    private String sessionOwnerID;
    private Integer questionCount;
    private List<QuestionModel> questions;
    private Integer currentQuestionIndex = 0;
}
