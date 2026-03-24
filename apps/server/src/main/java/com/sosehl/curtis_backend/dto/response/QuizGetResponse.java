package com.sosehl.curtis_backend.dto.response;

import com.sosehl.curtis_backend.dto.receive.QuestionCreateDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuizGetResponse {

    @NotBlank
    UUID uuid;

    @NotBlank
    String title;

    String description;

    @Valid
    List<QuestionCreateDto> questions;
}
