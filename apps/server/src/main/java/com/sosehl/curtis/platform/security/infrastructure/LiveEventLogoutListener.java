package com.sosehl.curtis.platform.security.infrastructure;

import com.sosehl.curtis.platform.realtime.application.LiveEventRegistry;
import com.sosehl.curtis.platform.security.application.CurrentUserService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class LiveEventLogoutListener {

    private final CurrentUserService currentUsers;
    private final LiveEventRegistry events;

    public LiveEventLogoutListener(
        CurrentUserService currentUsers,
        LiveEventRegistry events
    ) {
        this.currentUsers = currentUsers;
        this.events = events;
    }

    @EventListener
    public void disconnectLiveEvents(LogoutSuccessEvent event) {
        currentUsers.findIdentityKey(event.getAuthentication())
            .ifPresent(events::disconnectIdentity);
    }
}
