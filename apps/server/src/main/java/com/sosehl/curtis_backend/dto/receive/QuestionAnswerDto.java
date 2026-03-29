package com.sosehl.curtis_backend.dto.receive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class QuestionAnswerDto {

    @NotEmpty(message = "odpovězte alespoň jednu odpověď")
    private List<@Valid Integer> questionIndexes;
}
