package com.sosehl.curtis_backend.domain.v1.quizResult;

import java.util.List;
import lombok.Data;

@Data
public class ResultsResponse {

    private int score;
    private int maxScore;
    private List<QuestionResult> questions;
}
