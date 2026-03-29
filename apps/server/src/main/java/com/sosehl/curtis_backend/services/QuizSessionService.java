package com.sosehl.curtis_backend.services;

import com.sosehl.curtis_backend.components.Session;
import com.sosehl.curtis_backend.dto.response.QuestionResponse;
import com.sosehl.curtis_backend.dto.response.ResultsResponse;
import com.sosehl.curtis_backend.mappers.QuizMapper;
import com.sosehl.curtis_backend.models.QuestionModel;
import com.sosehl.curtis_backend.models.QuizModel;
import com.sosehl.curtis_backend.repositories.QuizRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuizSessionService {

    private final QuizRepository quizRepository;
    private final ConcurrentHashMap<UUID, Session> liveSessions =
        new ConcurrentHashMap<>();

    public QuizSessionService(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    public QuestionResponse start(UUID quizUuid, OAuth2User principal) {
        String userId = principal.getAttribute("sub");

        QuizModel quiz = quizRepository
            .findByUuid(quizUuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kvíz nebyl nalezen"
                )
            );

        List<QuestionModel> questions = quiz.getQuestions();
        if (questions == null || questions.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Kvíz neobsahuje žádné otázky"
            );
        }

        Session session = new Session()
            .setQuiz(QuizMapper.mapGetResponse(quiz))
            .setUserId(userId);
        session.build();

        UUID sessionUUID = UUID.randomUUID();
        liveSessions.put(sessionUUID, session);
        session.setSessionUuid(sessionUUID);
        return session.nextQuestion();
    }

    public QuestionResponse next(UUID sessionUUID, List<Integer> answers) {
        Session session = liveSessions.get(sessionUUID);
        if (session == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Session nenalezena"
            );
        }
        session.setUserAnswer(answers);
        return session.nextQuestion();
    }

    public void submitAnswer(UUID sessionUUID, List<Integer> userAnswer) {
        Session session = liveSessions.get(sessionUUID);
        if (session == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Session nenalezena"
            );
        }

        session.setUserAnswer(userAnswer);
    }

    // UNUSED
    public void removeArchivedSessions() {
        liveSessions
            .entrySet()
            .removeIf(
                entry ->
                    entry.getValue().getState() ==
                    com.sosehl.curtis_backend.enums.SessionState.ARCHIVED
            );
    }

    public ResultsResponse getResults(UUID sessionUUID) {
        Session session = liveSessions.get(sessionUUID);
        return session.getResults();
    }
}
