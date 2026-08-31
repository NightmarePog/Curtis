package com.sosehl.curtis_backend.domain.v1.quiz.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sosehl.curtis_backend.domain.v1.question.MatchingPair;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class QuestionYamlDto {

    @NotBlank(message = "Musíte zadat otázku")
    private String question;

    private QuestionType type = QuestionType.MULTIPLE_CHOICE;

    @NotNull(message = "Body musí být vyplněny")
    @Min(value = 1, message = "Body musí být alespoň 1")
    private Integer points = 1;

    @Size(max = 20000, message = "Code snippet je příliš dlouhý")
    private String codeSnippet;

    @JsonAlias({ "image", "imageFilename" })
    @Size(max = 255, message = "Název obrázku je příliš dlouhý")
    @Pattern(
        regexp = "^[^/\\\\]+$",
        message = "imageRef musí být název souboru bez cesty"
    )
    private String imageRef;

    @NotNull(message = "Čas musí být vyplněn")
    @Min(value = 1, message = "Čas nemůže být menší než jedna vteřina")
    private Integer timeInSeconds;

    @Size(min = 2, message = "Otázka musí mít alespoň dvě možnosti")
    private List<String> options;

    private List<Integer> correctIndexes;

    @Valid
    private List<MatchingPair> pairs;

    @JsonIgnore
    @AssertTrue(message = "Obsah otázky neodpovídá jejímu typu")
    public boolean isContentValid() {
        if (type == null) return false;
        boolean hasOptions = options != null && !options.isEmpty();
        boolean hasCorrectIndexes =
            correctIndexes != null && !correctIndexes.isEmpty();
        boolean hasPairs = pairs != null && !pairs.isEmpty();
        return switch (type) {
            case MULTIPLE_CHOICE -> hasOptions && hasCorrectIndexes && !hasPairs;
            case MATCHING -> hasPairs && !hasOptions && !hasCorrectIndexes;
            case FREE_TEXT -> !hasOptions && !hasCorrectIndexes && !hasPairs;
        };
    }
}
