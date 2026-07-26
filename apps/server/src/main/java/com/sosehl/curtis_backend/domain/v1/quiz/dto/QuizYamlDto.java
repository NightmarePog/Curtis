package com.sosehl.curtis_backend.domain.v1.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuizYamlDto {

    private UUID uuid;

    @NotBlank(message = "Musíte zadat název kvízu")
    @Size(min = 1, max = 100, message = "Název musí mít 1 až 100 znaků")
    private String title;

    private String description;

    @NotNull(message = "maxQuestionsPerSession je povinný")
    private Integer maxQuestionsPerSession;

    private Boolean shuffle = false;

    @NotEmpty(message = "Kvíz musí mít alespoň jednu otázku")
    @Valid
    private List<QuestionYamlDto> questions;
}
