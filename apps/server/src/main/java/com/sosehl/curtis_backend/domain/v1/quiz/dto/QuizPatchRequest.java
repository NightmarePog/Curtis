package com.sosehl.curtis_backend.domain.v1.quiz.dto;

import com.sosehl.curtis_backend.domain.v1.quiz.QuizStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class QuizPatchRequest {

    @Size(min = 1, max = 100, message = "Název musí mít 1 až 100 znaků")
    private String title;

    private String description;

    private Integer maxQuestionsPerSession;

    private Boolean shuffle;

    private QuizStatus status;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;
}
