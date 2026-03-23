package com.sosehl.curtis_backend.models;

import jakarta.persistence.*;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "questions")
public class QuestionsModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
