package com.sosehl.curtis.feature.sessions;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionExpiryScheduler {

    private final SessionService sessions;

    public SessionExpiryScheduler(SessionService sessions) {
        this.sessions = sessions;
    }

    @Scheduled(fixedDelay = 30000)
    public void expireDueSessions() {
        sessions.expireDueSessions();
    }
}
