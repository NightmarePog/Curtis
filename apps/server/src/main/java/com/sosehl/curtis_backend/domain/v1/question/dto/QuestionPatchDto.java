package com.sosehl.curtis_backend.domain.v1.question.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.MatchingPair;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class QuestionPatchDto {

    @NotEmpty(message = "je třeba ID otázky")
    private Long questionId;

    private String question;

    @Valid
    private List<QuestionAnswer> answers;

    private QuestionType type;

    @Min(value = 1, message = "Body musí být alespoň 1")
    private Integer points;

    @Size(max = 20000, message = "Code snippet je příliš dlouhý")
    private String codeSnippet;

    @Size(max = 255, message = "Název obrázku je příliš dlouhý")
    @Pattern(
        regexp = "^[^/\\\\]+$",
        message = "imageRef musí být název souboru bez cesty"
    )
    private String imageRef;

    @Valid
    private List<MatchingPair> pairs;

    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Integer timeInSeconds;

    @JsonIgnore
    @AssertTrue(message = "Obsah otázky neodpovídá jejímu typu")
    public boolean isContentValid() {
        if (type == null) return true;
        boolean hasAnswers = answers != null && !answers.isEmpty();
        boolean hasPairs = pairs != null && !pairs.isEmpty();
        return switch (type) {
            case MULTIPLE_CHOICE -> hasAnswers && !hasPairs;
            case MATCHING -> hasPairs && !hasAnswers;
            case FREE_TEXT -> !hasAnswers && !hasPairs;
        };
    }
}
