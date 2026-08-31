package com.sosehl.curtis_backend.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.sosehl.curtis_backend.domain.v1.question.MatchingPair;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionCreateDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestionFeatureTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void defaultsCreateQuestionToMultipleChoiceWithOnePoint() {
        QuestionCreateDto dto = new QuestionCreateDto();

        assertThat(dto.getType()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        assertThat(dto.getPoints()).isEqualTo(1);
    }

    @Test
    void acceptsMatchingQuestionWithPairsAndRejectsMissingPairs() {
        QuestionCreateDto dto = new QuestionCreateDto();
        dto.setQuestion("Match the terms");
        dto.setType(QuestionType.MATCHING);
        dto.setTimeInSeconds(30);
        dto.setPairs(
            List.of(new MatchingPair("one", "1"), new MatchingPair("two", "2"))
        );

        assertThat(validator.validate(dto)).isEmpty();

        dto.setPairs(List.of());
        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void rejectsImagePathsInsteadOfFilenames() {
        QuestionCreateDto dto = new QuestionCreateDto();
        dto.setQuestion("Question");
        dto.setTimeInSeconds(30);
        dto.setAnswers(List.of());
        dto.setImageRef("../secret.png");

        assertThat(validator.validate(dto))
            .extracting(v -> v.getPropertyPath().toString())
            .contains("imageRef");
    }

    @Test
    void rejectsNonPositivePointsAndOversizedCodeSnippets() {
        QuestionCreateDto dto = new QuestionCreateDto();
        dto.setQuestion("Question");
        dto.setTimeInSeconds(30);
        dto.setPoints(0);
        dto.setCodeSnippet("x".repeat(20_001));

        assertThat(validator.validate(dto))
            .extracting(v -> v.getPropertyPath().toString())
            .contains("points", "codeSnippet");
    }
}
