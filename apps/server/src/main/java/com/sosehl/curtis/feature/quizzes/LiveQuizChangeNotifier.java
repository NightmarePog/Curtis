package com.sosehl.curtis.feature.quizzes;

import com.sosehl.curtis.feature.quizzes.core.QuizChangeNotifier;
import com.sosehl.curtis.platform.realtime.application.LiveEventPublisher;
import com.sosehl.curtis.platform.realtime.domain.LiveEventType;
import com.sosehl.curtis.platform.realtime.domain.LiveInvalidation;
import org.springframework.stereotype.Component;

@Component
public class LiveQuizChangeNotifier implements QuizChangeNotifier {

    private final LiveEventPublisher events;

    public LiveQuizChangeNotifier(LiveEventPublisher events) {
        this.events = events;
    }

    @Override
    public void quizzesChanged() {
        events.publish(LiveInvalidation.broadcast(LiveEventType.QUIZZES_CHANGED));
    }
}
