package com.sosehl.curtis_backend.domain.v1.quiz;

import com.sosehl.curtis_backend.domain.v1.question.Question;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;

@Data
@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    private UUID uuid = UUID.randomUUID();

    private String title;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime editedAt;

    @OneToMany(
        mappedBy = "quiz",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Question> questions;

    private Integer maxQuestionsPerSession;

    private Boolean shuffle;
}
