package com.sosehl.curtis_backend.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class ResultsResponse {

    private Integer score;
    private Integer maxScore;
    private List<QuestionResponse> questions;
    private List<List<Integer>> answers;
}
