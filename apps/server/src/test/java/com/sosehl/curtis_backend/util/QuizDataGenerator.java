package com.sosehl.curtis_backend.util;

import com.sosehl.curtis_backend.dto.quiz.QuestionDto;
import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuizDataGenerator {

    private int questionCount = 1;
    private final Random random = new Random();

    public QuizDataGenerator setQuestionCount(int count) {
        this.questionCount = count;
        return this;
    }

    public int getQuestionCount() {
        return this.questionCount;
    }

    public QuizCreateRequest build() {
        QuizCreateRequest quiz = new QuizCreateRequest();
        quiz.setTitle("Quiz " + random.nextInt(1000));
        quiz.setDescription("Randomly generated quiz");

        List<QuestionDto> questions = new ArrayList<>();

        for (int i = 0; i < questionCount; i++) {
            questions.add(generateRandomQuestion(i + 1));
        }

        quiz.setQuestions(questions);
        return quiz;
    }

    private QuestionDto generateRandomQuestion(int number) {
        QuestionDto q = new QuestionDto();
        q.setQuestion("Question " + number + ": What is " + randomWord() + "?");

        List<String> answers = new ArrayList<>();
        answers.add(randomWord());
        answers.add(randomWord());
        answers.add(randomWord());
        q.setPossibleAnswers(answers);

        q.setAnswersId(List.of(random.nextInt(answers.size())));

        q.setTime(10 + random.nextInt() * 50);

        return q;
    }

    private String randomWord() {
        String[] words = {
            "Java",
            "Python",
            "Spring",
            "Hibernate",
            "Database",
            "Cloud",
            "Docker",
        };
        return words[random.nextInt(words.length)];
    }
}
