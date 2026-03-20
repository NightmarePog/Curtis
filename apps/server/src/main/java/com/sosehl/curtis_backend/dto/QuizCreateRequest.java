package com.sosehl.curtis_backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuizCreateRequest {

    @NotBlank
    private String uuid;
    @NotBlank
    @Size(min = 1, max = 100)

    private String title;
    
    @NotBlank
    private String description;
    @NotBlank
    private List<QuestionDto> questions;

   
}
