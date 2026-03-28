package com.sosehl.curtis_backend.dto.response;

import com.sosehl.curtis_backend.dto.receive.QuestionCreateDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuizGetResponse {

    UUID uuid;
    String title;
    String description;
    List<QuestionResponse> questions;
}
