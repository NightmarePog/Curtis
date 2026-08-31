package com.sosehl.curtis_backend.domain.v1.session;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.MatchingPair;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.session.exceptions.NoMoreQuestionsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StudentAttempt {

    private static final int GRACE_PERIOD_SECONDS = 2;

    private final String studentId;
    private final List<QuestionResponse> questions;
    private final List<QuestionSubmission> submissions = new ArrayList<>();
    private final Clock clock;
    private int questionIndex = 0;
    private SessionStatus status = SessionStatus.RUNNING;
    private Instant servedAt;

    public StudentAttempt(String studentId, List<QuestionResponse> questions) {
        this(studentId, questions, Clock.systemDefaultZone());
    }

    public StudentAttempt(
        String studentId,
        List<QuestionResponse> questions,
        Clock clock
    ) {
        this.studentId = studentId;
        this.questions = questions;
        this.clock = clock;
    }

    public String getStudentId() {
        return studentId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public List<QuestionResponse> getQuestions() {
        return questions;
    }

    public List<List<Integer>> getAnswers() {
        return submissions
            .stream()
            .map(submission -> {
                List<Integer> indexes = submission.getSelectedIndexes();
                return indexes == null ? new ArrayList<Integer>() : indexes;
            })
            .toList();
    }

    public QuestionResponse nextQuestion() {
        if (questionIndex >= questions.size()) {
            this.status = SessionStatus.ARCHIVED;
            throw new NoMoreQuestionsException("Žádné další otázky");
        }
        if (status != SessionStatus.RUNNING) {
            throw new IllegalStateException("Pokus již byl ukončen");
        }

        QuestionResponse question = questions.get(questionIndex);
        questionIndex++;
        servedAt = clock.instant();

        if (questionIndex >= questions.size()) {
            this.status = SessionStatus.ARCHIVED;
        }

        return question;
    }

    public void addAnswer(List<Integer> answer) {
        QuestionSubmission submission = QuestionSubmission.multipleChoice(answer);
        if (questionIndex > 0 && effectiveType(questions.get(questionIndex - 1)) == QuestionType.FREE_TEXT) {
            submission.setType(QuestionType.FREE_TEXT);
            submission.setText("");
        }
        addSubmission(submission);
    }

    public void addSubmission(QuestionSubmission submission) {
        QuestionResponse question = questions.get(questionIndex - 1);
        if (isAnswerLate()) {
            QuestionSubmission late = new QuestionSubmission();
            late.setType(effectiveType(question));
            late.setSelectedIndexes(new ArrayList<>());
            late.setPairs(new ArrayList<>());
            submissions.add(late);
            return;
        }

        validateSubmission(question, submission);
        submissions.add(submission);
    }

    private boolean isAnswerLate() {
        if (servedAt == null || questionIndex == 0) {
            return false;
        }

        Integer timeInSeconds = questions
            .get(questionIndex - 1)
            .getTimeInSeconds();
        if (timeInSeconds == null) {
            return false;
        }

        Duration allowed = Duration.ofSeconds(
            timeInSeconds + GRACE_PERIOD_SECONDS
        );
        Duration elapsed = Duration.between(servedAt, clock.instant());
        return elapsed.compareTo(allowed) > 0;
    }

    public StudentAttempt finish() {
        this.status = SessionStatus.ARCHIVED;
        return this;
    }

    public int calculateScore() {
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            Integer awarded = autoAwardedPoints(i);
            if (awarded != null) score += awarded;
        }
        return score;
    }

    public Integer autoAwardedPoints(int questionIndex) {
        if (questionIndex < 0 || questionIndex >= questions.size()) {
            throw new IllegalArgumentException("Neplatný index otázky");
        }
        QuestionResponse question = questions.get(questionIndex);
        QuestionType type = effectiveType(question);
        if (type == QuestionType.FREE_TEXT) return null;
        if (submissions.size() <= questionIndex) return 0;

        QuestionSubmission submission = submissions.get(questionIndex);
        boolean correct = type == QuestionType.MULTIPLE_CHOICE
            ? isMultipleChoiceCorrect(question, submission)
            : isMatchingCorrect(question, submission);
        return correct ? pointsFor(question) : 0;
    }

    public List<QuestionSubmission> getSubmissions() {
        return submissions;
    }

    private void validateSubmission(
        QuestionResponse question,
        QuestionSubmission submission
    ) {
        if (submission == null || submission.getType() == null) {
            throw new IllegalArgumentException("Typ odpovědi musí být vyplněn");
        }

        QuestionType type = effectiveType(question);
        if (submission.getType() != type) {
            throw new IllegalArgumentException("Typ odpovědi neodpovídá otázce");
        }

        if (type == QuestionType.MULTIPLE_CHOICE) {
            List<Integer> indexes = submission.getSelectedIndexes();
            if (indexes == null || new HashSet<>(indexes).size() != indexes.size()) {
                throw new IllegalArgumentException("selectedIndexes jsou neplatné");
            }
            int answerCount = question.getAnswers() == null ? 0 : question.getAnswers().size();
            if (indexes.stream().anyMatch(index -> index == null || index < 0 || index >= answerCount)) {
                throw new IllegalArgumentException("selectedIndexes obsahuje neplatný index");
            }
        } else if (type == QuestionType.MATCHING) {
            List<MatchingSubmissionPair> pairs = submission.getPairs();
            int pairCount = question.getPairs() == null ? 0 : question.getPairs().size();
            if (pairs == null || pairs.size() != pairCount) {
                throw new IllegalArgumentException("pairs musí obsahovat všechny dvojice");
            }
            Set<Integer> leftIndexes = new HashSet<>();
            Set<Integer> rightIndexes = new HashSet<>();
            for (MatchingSubmissionPair pair : pairs) {
                if (
                    pair == null ||
                    pair.getLeftIndex() == null ||
                    pair.getRightIndex() == null ||
                    pair.getLeftIndex() < 0 ||
                    pair.getLeftIndex() >= pairCount ||
                    pair.getRightIndex() < 0 ||
                    pair.getRightIndex() >= pairCount ||
                    !leftIndexes.add(pair.getLeftIndex()) ||
                    !rightIndexes.add(pair.getRightIndex())
                ) {
                    throw new IllegalArgumentException("pairs obsahují neplatné indexy");
                }
            }
        } else if (submission.getText() == null) {
            throw new IllegalArgumentException("Textová odpověď musí být vyplněna");
        }
    }

    private boolean isMultipleChoiceCorrect(
        QuestionResponse question,
        QuestionSubmission submission
    ) {
        List<QuestionAnswer> answers = question.getAnswers() == null
            ? List.of()
            : question.getAnswers();
        List<Integer> correct = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            if (Boolean.TRUE.equals(answers.get(i).getIsCorrect())) {
                correct.add(i);
            }
        }
        return new HashSet<>(correct).equals(new HashSet<>(submission.getSelectedIndexes()));
    }

    private boolean isMatchingCorrect(
        QuestionResponse question,
        QuestionSubmission submission
    ) {
        List<MatchingPair> pairs = question.getPairs() == null
            ? List.of()
            : question.getPairs();
        Map<Integer, Integer> expected = new java.util.HashMap<>();
        for (int i = 0; i < pairs.size(); i++) {
            expected.put(i, i);
        }
        Map<Integer, Integer> actual = submission
            .getPairs()
            .stream()
            .collect(Collectors.toMap(
                MatchingSubmissionPair::getLeftIndex,
                MatchingSubmissionPair::getRightIndex
            ));
        return expected.equals(actual);
    }

    private QuestionType effectiveType(QuestionResponse question) {
        return question.getType() == null
            ? QuestionType.MULTIPLE_CHOICE
            : question.getType();
    }

    private int pointsFor(QuestionResponse question) {
        return question.getPoints() == null ? 1 : question.getPoints();
    }
}
