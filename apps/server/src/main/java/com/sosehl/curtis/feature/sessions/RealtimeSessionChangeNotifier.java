package com.sosehl.curtis.feature.sessions;

import com.sosehl.curtis.feature.sessions.core.SessionChangeNotifier;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.platform.realtime.application.LiveEventPublisher;
import com.sosehl.curtis.platform.realtime.domain.LiveEventType;
import com.sosehl.curtis.platform.realtime.domain.LiveInvalidation;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RealtimeSessionChangeNotifier implements SessionChangeNotifier {

    private final LiveEventPublisher events;
    private final UserDirectory users;

    public RealtimeSessionChangeNotifier(
        LiveEventPublisher events,
        UserDirectory users
    ) {
        this.events = events;
        this.users = users;
    }

    @Override
    public void sessionsChanged(Set<UUID> recipients) {
        events.publish(
            LiveInvalidation.to(LiveEventType.SESSIONS_CHANGED, recipients)
        );
    }

    @Override
    public void resultsChanged(UUID studentId, UUID teacherId) {
        LinkedHashSet<UUID> recipients = new LinkedHashSet<>();
        recipients.add(studentId);
        recipients.add(teacherId);
        recipients.addAll(users.activeIdsWithRole(UserRole.ADMINISTRATOR));
        events.publish(
            LiveInvalidation.to(LiveEventType.RESULTS_CHANGED, recipients)
        );
    }
}
