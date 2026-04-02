package com.sosehl.curtis_backend.domain.v1.question;

import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import jakarta.persistence.*;
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

    @ElementCollection(fetch = FetchType.EAGER)
    private List<QuestionAnswer> answers;

    private Integer timeInSeconds;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;
}
