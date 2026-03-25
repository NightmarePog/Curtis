package com.sosehl.curtis_backend.dto.receive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class QuestionCreateDto {

    @NotBlank(message = "Musíte zadat otázku")
    @Size(min = 1, message = "Otázka musí mít alespoň 1 znak")
    private String question;

    @NotEmpty(message = "Musíte zadat možné odpovědi")
    @Valid
    private List<String> answers;

    @NotEmpty(message = "Musíte zadat alespoň jednu správnou odpověď")
    private List<Integer> correctAnswers;

    @NotEmpty(message = "Musíte zadat alespoň jednu správnou odpověď")
    private List<Integer> answersId;

    @NotNull(message = "Čas musí být vyplněn")
    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Integer time;

    @NotNull(message = "Pořadí musí být vyplněno")
    @Min(value = 0, message = "Pořadí nemůže být menší než 0")
    private Integer questionOrder;
}
