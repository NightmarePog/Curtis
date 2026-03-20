package com.sosehl.curtis_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class QuestionDto {

    @NotBlank(message = "Musíte zadat otázku")
    @Size(min = 1, message = "Otázka musí mít alespoň 1 znak")
    private String question;

    @NotEmpty(message = "Musíte zadat možné odpovědi")
    @Valid
    private List<AnswersDto> possibleAnswers;

    @NotEmpty(message = "Musíte zadat alespoň jednu správnou odpověď")
    private List<Integer> answersId;

    @NotNull(message = "Čas musí být vyplněn")
    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Float time;
}
