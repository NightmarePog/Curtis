package com.sosehl.curtis_backend.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionDto {

    @NotBlank
    private String uuid;

    @NotBlank(message = "Musíte zadat otázku")
    @Size(min = 1)
    private String question;
    
    @NotBlank
    private List<AnswersDto> possibleAnswers;

    @NotBlank
    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Float time;
}
