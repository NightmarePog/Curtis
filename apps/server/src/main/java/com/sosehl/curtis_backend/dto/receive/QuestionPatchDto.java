package com.sosehl.curtis_backend.dto.receive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class QuestionPatchDto {

    private String question; // volitelné

    @Valid
    private List<String> answer; // volitelné

    @Valid
    private List<Integer> correctAnswers; // volitelné

    @NotEmpty(message = "Musíte zadat alespoň jednu správnou odpověď")
    private List<Integer> answersId;

    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Integer time;

    @NotNull(message = "Původní pořadí musí být vyplněno")
    @Min(value = 0, message = "Pořadí nemůže být menší než 0")
    private Integer originalOrder;

    @Min(value = 0, message = "Pořadí nemůže být menší než 0")
    private Integer newOrder;
}
