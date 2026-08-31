package com.sosehl.curtis_backend.domain.v1.question;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;

    @Enumerated(EnumType.STRING)
    private QuestionType type = QuestionType.MULTIPLE_CHOICE;

    private Integer points = 1;

    @Column(length = 20000)
    private String codeSnippet;

    private String imageRef;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<QuestionAnswer> answers;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "question_matching_pairs",
        joinColumns = @JoinColumn(name = "question_id")
    )
    private List<MatchingPair> pairs = new ArrayList<>();

    private Integer timeInSeconds;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @PostLoad
    @PostPersist
    private void applyDefaults() {
        if (type == null) type = QuestionType.MULTIPLE_CHOICE;
        if (points == null) points = 1;
    }
}
