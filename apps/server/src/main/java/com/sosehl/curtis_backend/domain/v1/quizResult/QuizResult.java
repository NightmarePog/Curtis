package com.sosehl.curtis_backend.domain.v1.quizResult;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "game_results")
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID sessionUuid;
    private UUID quizUuid;
    private String studentId;
    private int score;
    private int maxScore;
    private LocalDateTime playedAt;

    @OneToMany(
        mappedBy = "quizResult",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("questionIndex ASC")
    private List<QuestionResultEntity> questionResults = new ArrayList<>();
}
