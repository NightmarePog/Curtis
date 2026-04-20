package com.sosehl.curtis_backend.domain.v1.session;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizMapper;
import com.sosehl.curtis_backend.domain.v1.quiz.QuizRepository;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.quizResult.QuestionResult;
import com.sosehl.curtis_backend.domain.v1.quizResult.QuizResult;
import com.sosehl.curtis_backend.domain.v1.quizResult.QuizResultRepository;
import com.sosehl.curtis_backend.domain.v1.quizResult.ResultsResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
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
        return session.nextQuestion(studentId);
    }

    public QuestionResponse next(
        UUID sessionUuid,
        List<Integer> answer,
        OAuth2User principal
    ) {
        String studentId = principal.getAttribute("sub");
        Session session = getSession(sessionUuid);
        session.submitAnswer(studentId, answer);
        return session.nextQuestion(studentId);
    }

    public ResultsResponse finish(UUID sessionUuid, OAuth2User principal) {
        String studentId = principal.getAttribute("sub");
        Session session = getSession(sessionUuid);
        StudentAttempt attempt = session.finishAttempt(studentId);

        QuizResult result = new QuizResult();
        result.setSessionUuid(sessionUuid);
        result.setStudentId(studentId);
        result.setScore(attempt.calculateScore());
        result.setMaxScore(attempt.getQuestions().size());
        result.setPlayedAt(LocalDateTime.now());
        quizResultRepository.save(result);

        List<QuestionResult> questionResults = attempt
            .getQuestions()
            .stream()
            .map(q -> {
                QuestionResult qr = new QuestionResult();
                qr.setQuestion(q.getQuestion());
                qr.setAnswers(q.getAnswers());
                return qr;
            })
            .toList();

        ResultsResponse response = new ResultsResponse();
        response.setScore(result.getScore());
        response.setMaxScore(result.getMaxScore());
        response.setQuestions(questionResults);
        return response;
    }

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
}
