package com.sosehl.curtis_backend.domain.v1.quizResult;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import com.sosehl.curtis_backend.domain.v1.session.MatchingSubmissionPair;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "question_results")
public class QuestionResultEntity {

    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String GRADED = "GRADED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "quiz_result_id", nullable = false)
    private QuizResult quizResult;

    private int questionIndex;
    private String question;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    private int points;
    private Integer awardedPoints;
    private String status;

    @Column(length = 20000)
    private String text;

    @ElementCollection
    @CollectionTable(
        name = "question_result_selected_indexes",
        joinColumns = @JoinColumn(name = "question_result_id")
    )
    @Column(name = "selected_index")
    private List<Integer> selectedIndexes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "question_result_matching_pairs",
        joinColumns = @JoinColumn(name = "question_result_id")
    )
    private List<MatchingSubmissionPair> pairs = new ArrayList<>();
}
