/*

package com.sosehl.curtis_backend.models;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "quizzes")
public class QuizModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    private String title;

    private String description;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<QuestionsModel> questions;
}


*/

package com.sosehl.curtis_backend.dto.response;

import com.sosehl.curtis_backend.dto.quiz.QuestionDto;
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
    List<QuestionDto> questions;
}
