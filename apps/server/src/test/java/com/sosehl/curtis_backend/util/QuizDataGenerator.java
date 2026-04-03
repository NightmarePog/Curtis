package com.sosehl.curtis_backend.util;

import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import java.util.Random;

public class QuizDataGenerator {

    private final Random random = new Random();

    public QuizCreateRequest build() {
        QuizCreateRequest quiz = new QuizCreateRequest();
        quiz.setTitle("Quiz " + random.nextInt(1000));
        quiz.setDescription("Randomly generated quiz");

        return quiz;
    }
}
