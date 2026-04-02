package com.sosehl.curtis_backend.domain.v1.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class QuizPatchRequest {

    @NotNull(message = "Neznámá otázka")
    private UUID uuid;

    @Size(min = 1, max = 100, message = "Název musí mít 1 až 100 znaků")
    private String title;

    private String description;

    @NotBlank
    private LocalDateTime expireAt;
}
