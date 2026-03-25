package com.sosehl.curtis_backend.models;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "questions")
public class QuestionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OrderColumn
    private Integer questionOrder;

    private String question;

    @ElementCollection
    private List<Integer> correctAnswers;

    @ElementCollection
    private List<String> answers;

    private Integer time;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private QuizModel quiz;
}
