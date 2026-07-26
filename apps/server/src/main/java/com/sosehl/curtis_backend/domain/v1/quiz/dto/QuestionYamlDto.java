package com.sosehl.curtis_backend.domain.v1.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class QuestionYamlDto {

    @NotBlank(message = "Musíte zadat otázku")
    private String question;

    @NotNull(message = "Čas musí být vyplněn")
    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Integer timeInSeconds;

    @NotNull(message = "Musíte zadat možné odpovědi")
    @Size(min = 2, message = "Otázka musí mít alespoň dvě možnosti")
    private List<String> options;

    @NotEmpty(message = "Musíte označit alespoň jednu správnou odpověď")
    private List<Integer> correctIndexes;
}
