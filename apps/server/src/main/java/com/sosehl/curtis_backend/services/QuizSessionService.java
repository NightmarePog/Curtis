package com.sosehl.curtis_backend.services;

import com.sosehl.curtis_backend.dto.response.QuestionResponse;
import com.sosehl.curtis_backend.mappers.QuizSessionMapper;
import com.sosehl.curtis_backend.models.QuestionModel;
import com.sosehl.curtis_backend.models.QuizModel;
import com.sosehl.curtis_backend.models.QuizSessionModel;
import com.sosehl.curtis_backend.repositories.QuizRepository;
import java.util.Collections;
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
    private final ConcurrentHashMap<UUID, QuizSessionModel> liveSessions =
        new ConcurrentHashMap<>();
    private static final long SESSION_TTL_MINUTES = 30;

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

        Collections.shuffle(questions);

        QuizSessionModel session = QuizSessionMapper.toSession(
            userId,
            questions
        );
        session.setCurrentQuestionIndex(0);
        liveSessions.put(session.getSessionUUID(), session);

        QuestionModel firstQuestion = questions.get(0);
        return QuizSessionMapper.toQuestionResponse(firstQuestion);
    }

    public QuestionResponse next(UUID sessionUUID) {
        QuizSessionModel session = getValidSession(sessionUUID);

        int currentIndex = session.getCurrentQuestionIndex();
        if (currentIndex >= session.getQuestions().size()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Žádné další otázky"
            );
        }

        QuestionModel questionModel = session.getQuestions().get(currentIndex);
        session.setCurrentQuestionIndex(currentIndex + 1);

        return QuizSessionMapper.toQuestionResponse(questionModel);
    }

    public List<QuizSessionModel> getLiveSessions() {
        return Collections.unmodifiableList(List.copyOf(liveSessions.values()));
    }

    public void result(UUID sessionUUID) {
        getValidSession(sessionUUID);
    }

    private QuizSessionModel getValidSession(UUID sessionUUID) {
        QuizSessionModel session = liveSessions.get(sessionUUID);
        if (session == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Session nenalezena"
            );
        }
        return session;
    }
}
