package com.sosehl.curtis_backend.dto.receive;

import com.sosehl.curtis_backend.dto.quiz.QuestionPatchDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuizPatchRequest {

    @NotNull
    UUID uuid;

    @Size(min = 1, max = 100, message = "Název musí mít 1 až 100 znaků")
    private String title;

    private String description;

    private List<QuestionPatchDto> questions;
}
