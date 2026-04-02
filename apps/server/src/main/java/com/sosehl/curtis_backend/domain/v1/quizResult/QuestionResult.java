package com.sosehl.curtis_backend.domain.v1.quizResult;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import java.util.List;
import lombok.Data;

@Data
public class QuestionResult {

    private String question;
    private List<QuestionAnswer> answers;
}
