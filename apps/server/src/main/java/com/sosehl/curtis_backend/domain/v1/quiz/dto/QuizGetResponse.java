package com.sosehl.curtis_backend.domain.v1.quiz.dto;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuizGetResponse {

    UUID uuid;
    String title;
    String description;
    List<QuestionResponse> questions;
    Integer ExpiresInMinutes;
}
