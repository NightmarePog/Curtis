package com.sosehl.curtis_backend.domain.v1.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuizCreateRequest {

    @NotBlank(message = "Title je povinný")
    @Size(min = 1, max = 100, message = "Název musí mít 1 až 100 znaků")
    private String title;

    private String description;

    @NotNull(message = "maxQuestionsPerSession je povinný")
    private Integer maxQuestionsPerSession;

    private Boolean shuffle = false;
}
