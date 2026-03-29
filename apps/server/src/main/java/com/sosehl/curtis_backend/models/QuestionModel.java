package com.sosehl.curtis_backend.models;

import jakarta.persistence.*;
import java.util.List;
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

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Integer> correctAnswers;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> answers;

    private Integer time;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private QuizModel quiz;
}
