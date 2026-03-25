package com.sosehl.curtis_backend.dto.receive;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuizCreateRequest {

    @NotBlank(message = "Title je povinný")
    @Size(min = 1, max = 100, message = "Název musí mít 1 až 100 znaků")
    private String title;

    private String description; // nepovinné

    @NotNull(message = "Pořadí je povinné")
    @Min(value = 0, message = "Pořadí nemůže být menší než 0")
    private Integer order;
}
