package com.sosehl.curtis_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnswersDto {
    @NotBlank
    private String uuid;
    @NotBlank
    @Size(min = 1)
    private String answer;
}
