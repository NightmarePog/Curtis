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
@Table(name = "question_options")
public class QuestionOptionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    protected QuestionOptionEntity() {}

    public QuestionOptionEntity(UUID id, int position, String text, boolean correct) {
        this.id = id;
        this.position = position;
        this.text = text;
        this.correct = correct;
    }

    void attachTo(QuestionEntity value) { question = value; }
    public UUID getId() { return id; }
    public int getPosition() { return position; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
}
