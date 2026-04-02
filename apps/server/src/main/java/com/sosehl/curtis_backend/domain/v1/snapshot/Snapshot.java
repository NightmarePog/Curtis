package com.sosehl.curtis_backend.domain.v1.snapshot;

import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "snapshots")
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String snapshotHash;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(columnDefinition = "TEXT")
    private String quizJson;

    public Snapshot(Quiz quiz, String quizJson, String hash) {
        this.quiz = quiz;
        this.quizJson = quizJson;
        this.snapshotHash = hash;
        this.createdAt = LocalDateTime.now();
    }

    protected Snapshot() {}
}
