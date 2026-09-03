package com.sosehl.curtis.feature.quizzes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "matching_pairs")
public class MatchingPairEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(nullable = false)
    private int position;

    @Column(name = "left_text", nullable = false, length = 1000)
    private String leftText;

    @Column(name = "right_text", nullable = false, length = 1000)
    private String rightText;

    protected MatchingPairEntity() {}

    public MatchingPairEntity(UUID id, int position, String left, String right) {
        this.id = id;
        this.position = position;
        this.leftText = left;
        this.rightText = right;
    }

    void attachTo(QuestionEntity value) { question = value; }
    public UUID getId() { return id; }
    public int getPosition() { return position; }
    public String getLeftText() { return leftText; }
    public String getRightText() { return rightText; }
}
