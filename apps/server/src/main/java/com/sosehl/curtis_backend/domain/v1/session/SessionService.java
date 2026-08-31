package com.sosehl.curtis_backend.domain.v1.session;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizMapper;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizRepository;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.quizResult.QuestionResult;
import com.sosehl.curtis_backend.domain.v1.quizResult.QuestionResultEntity;
import com.sosehl.curtis_backend.domain.v1.quizResult.QuizResult;
import com.sosehl.curtis_backend.domain.v1.quizResult.QuizResultRepository;
import com.sosehl.curtis_backend.domain.v1.quizResult.ResultsResponse;
import com.sosehl.curtis_backend.domain.v1.session.dto.PendingTextAnswerResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SessionService {

    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;
    private final QuizResultRepository quizResultRepository;
    private final ConcurrentHashMap<UUID, Session> liveSessions =
        new ConcurrentHashMap<>();

    public SessionService(
        QuizRepository quizRepository,
        QuizMapper quizMapper,
        QuizResultRepository quizResultRepository
    ) {
        this.quizRepository = quizRepository;
        this.quizMapper = quizMapper;
        this.quizResultRepository = quizResultRepository;
    }

    public UUID createSession(
        UUID quizUuid,
        Integer expiresInMinutes,
        OAuth2User principal
    ) {
        String adminId = principal.getAttribute("sub");

        QuizGetResponse quiz = quizRepository
            .findByUuid(quizUuid)
            .map(quizMapper::toResponse)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kvíz nebyl nalezen"
                )
            );

        Session session = new Session(adminId, quiz, expiresInMinutes);
        liveSessions.put(session.getUuid(), session);
        return session.getUuid();
    }

    public QuestionResponse join(UUID sessionUuid, OAuth2User principal) {
        String studentId = principal.getAttribute("sub");
        Session session = getSession(sessionUuid);
        session.join(studentId);
        return maskCorrectAnswers(session.nextQuestion(studentId));
    }

    public QuestionResponse next(
        UUID sessionUuid,
        QuestionSubmission submission,
        OAuth2User principal
    ) {
        String studentId = principal.getAttribute("sub");
        Session session = getSession(sessionUuid);
        session.submitAnswer(studentId, submission);
        return maskCorrectAnswers(session.nextQuestion(studentId));
    }

    @Transactional(readOnly = true)
    public List<QuizResult> getResults(UUID sessionUuid) {
        return quizResultRepository.findBySessionUuid(sessionUuid);
    }

    @Transactional(readOnly = true)
    public List<QuizResult> getStudentResults(String studentId) {
        return quizResultRepository.findByStudentIdOrderByPlayedAtDesc(studentId);
    }

    @Transactional
    public ResultsResponse finish(UUID sessionUuid, OAuth2User principal) {
        String studentId = principal.getAttribute("sub");
        Session session = getSession(sessionUuid);
        StudentAttempt attempt = session.finishAttempt(studentId);

        QuizResult result = new QuizResult();
        result.setSessionUuid(sessionUuid);
        result.setQuizUuid(session.getQuiz().getUuid());
        result.setStudentId(studentId);
        result.setScore(attempt.calculateScore());
        result.setMaxScore(
            attempt
                .getQuestions()
                .stream()
                .mapToInt(q -> q.getPoints() == null ? 1 : q.getPoints())
                .sum()
        );
        result.setPlayedAt(LocalDateTime.now());

        List<QuestionResult> questionResults = attempt.getQuestions().stream().map(q -> {
                QuestionResult qr = new QuestionResult();
                qr.setQuestion(q.getQuestion());
                qr.setType(q.getType());
                qr.setPoints(q.getPoints());
                qr.setCodeSnippet(q.getCodeSnippet());
                qr.setImageRef(q.getImageRef());
                qr.setAnswers(q.getAnswers());
                qr.setPairs(q.getPairs());
                return qr;
            })
            .toList();

        for (int i = 0; i < attempt.getQuestions().size(); i++) {
            var question = attempt.getQuestions().get(i);
            QuestionResultEntity persisted = new QuestionResultEntity();
            persisted.setQuizResult(result);
            persisted.setQuestionIndex(i);
            persisted.setQuestion(question.getQuestion());
            persisted.setType(question.getType());
            persisted.setPoints(question.getPoints() == null ? 1 : question.getPoints());

            if (attempt.getSubmissions().size() > i) {
                QuestionSubmission submission = attempt.getSubmissions().get(i);
                persisted.setSelectedIndexes(copyIndexes(submission.getSelectedIndexes()));
                persisted.setPairs(copyPairs(submission.getPairs()));
                persisted.setText(submission.getText());
            }

            Integer autoAwarded = attempt.autoAwardedPoints(i);
            if (question.getType() == QuestionType.FREE_TEXT
                && persisted.getText() != null
                && !persisted.getText().isBlank()) {
                persisted.setStatus(QuestionResultEntity.PENDING_REVIEW);
            } else {
                persisted.setStatus(QuestionResultEntity.GRADED);
                persisted.setAwardedPoints(autoAwarded == null ? 0 : autoAwarded);
            }
            result.getQuestionResults().add(persisted);
        }
        quizResultRepository.save(result);

        ResultsResponse response = new ResultsResponse();
        response.setScore(result.getScore());
        response.setMaxScore(result.getMaxScore());
        response.setQuestions(questionResults);
        return response;
    }

    @Transactional(readOnly = true)
    public List<PendingTextAnswerResponse> getPendingTextAnswers(UUID sessionUuid) {
        List<PendingTextAnswerResponse> pending = new ArrayList<>();
        for (QuizResult result : quizResultRepository.findBySessionUuid(sessionUuid)) {
            for (QuestionResultEntity question : result.getQuestionResults()) {
                if (!QuestionResultEntity.PENDING_REVIEW.equals(question.getStatus())) continue;
                PendingTextAnswerResponse response = new PendingTextAnswerResponse();
                response.setResultId(question.getId());
                response.setStudentId(result.getStudentId());
                response.setQuestionIndex(question.getQuestionIndex());
                response.setQuestion(question.getQuestion());
                response.setText(question.getText());
                response.setPoints(question.getPoints());
                response.setAwardedPoints(question.getAwardedPoints());
                response.setStatus(question.getStatus());
                pending.add(response);
            }
        }
        return pending;
    }

    @Transactional
    public PendingTextAnswerResponse awardTextPoints(
        UUID sessionUuid,
        Long resultId,
        int awardedPoints
    ) {
        QuizResult result = quizResultRepository
            .findBySessionUuid(sessionUuid)
            .stream()
            .filter(candidate -> candidate.getQuestionResults().stream().anyMatch(q -> resultId.equals(q.getId())))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Výsledek nebyl nalezen"));
        QuestionResultEntity question = result.getQuestionResults().stream()
            .filter(candidate -> resultId.equals(candidate.getId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Výsledek nebyl nalezen"));
        if (question.getType() != QuestionType.FREE_TEXT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Výsledek není textová odpověď");
        }
        if (awardedPoints < 0 || awardedPoints > question.getPoints()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Udělěné body jsou vyšší než maximum");
        }

        int previous = question.getAwardedPoints() == null ? 0 : question.getAwardedPoints();
        question.setAwardedPoints(awardedPoints);
        question.setStatus(QuestionResultEntity.GRADED);
        result.setScore(result.getScore() - previous + awardedPoints);
        quizResultRepository.save(result);

        PendingTextAnswerResponse response = new PendingTextAnswerResponse();
        response.setResultId(question.getId());
        response.setStudentId(result.getStudentId());
        response.setQuestionIndex(question.getQuestionIndex());
        response.setQuestion(question.getQuestion());
        response.setText(question.getText());
        response.setPoints(question.getPoints());
        response.setAwardedPoints(question.getAwardedPoints());
        response.setStatus(question.getStatus());
        return response;
    }

    @Scheduled(fixedRate = 60000)
    public void removeExpiredSessions() {
        liveSessions.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private Session getSession(UUID uuid) {
        Session session = liveSessions.get(uuid);
        if (session == null) throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Session nenalezena"
        );
        return session;
    }

    private QuestionResponse maskCorrectAnswers(QuestionResponse question) {
        if (question == null || question.getAnswers() == null) {
            return question;
        }
        QuestionResponse masked = new QuestionResponse();
        masked.setQuestion(question.getQuestion());
        masked.setType(question.getType());
        masked.setPoints(question.getPoints());
        masked.setCodeSnippet(question.getCodeSnippet());
        masked.setImageRef(question.getImageRef());
        masked.setTimeInSeconds(question.getTimeInSeconds());
        masked.setQuizUuid(question.getQuizUuid());
        masked.setPairs(question.getPairs());
        masked.setAnswers(
            question
                .getAnswers()
                .stream()
                .map(answer -> {
                    QuestionAnswer clone = new QuestionAnswer();
                    clone.setAnswer(answer.getAnswer());
                    clone.setIsCorrect(null);
                    return clone;
                })
                .toList()
        );
        return masked;
    }

    private List<Integer> copyIndexes(List<Integer> indexes) {
        return indexes == null ? new ArrayList<>() : new ArrayList<>(indexes);
    }

    private List<MatchingSubmissionPair> copyPairs(
        List<MatchingSubmissionPair> pairs
    ) {
        return pairs == null ? new ArrayList<>() : new ArrayList<>(pairs);
    }
}
