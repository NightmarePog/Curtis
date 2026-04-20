package com.sosehl.curtis_backend.domain.v1.question;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class QuestionAnswer {

    Boolean isCorrect;
    String answer;
}
