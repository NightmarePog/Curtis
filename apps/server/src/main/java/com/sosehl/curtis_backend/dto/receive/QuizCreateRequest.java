package com.sosehl.curtis_backend.dto.receive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class QuizCreateRequest {

    @NotBlank
    @Size(min = 1, max = 100, message = "Název musí mít 1 až 100 znaků")
    private String title;

    private String description;
}
