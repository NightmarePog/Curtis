package com.sosehl.curtis_backend.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class QuestionPatchDto {

    @Size(min = 1, message = "Otázka musí mít alespoň 1 znak")
    private String question;

    @Valid
    private List<String> possibleAnswers;

    @NotEmpty(message = "Musíte zadat alespoň jednu správnou odpověď")
    private List<Integer> answersId;

    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Integer time;
}
