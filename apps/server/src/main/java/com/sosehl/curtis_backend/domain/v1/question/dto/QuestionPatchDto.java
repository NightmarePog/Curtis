package com.sosehl.curtis_backend.domain.v1.question.dto;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class QuestionPatchDto {

    @NotEmpty(message = "je třeba ID otázky")
    private Long questionId;

    private String question;

    @Valid
    private List<QuestionAnswer> answers;

    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Integer timeInSeconds;
}
