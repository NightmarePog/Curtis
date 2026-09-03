package com.sosehl.curtis.feature.classrooms;

import com.sosehl.curtis.feature.classrooms.core.ClassroomChangeNotifier;
import com.sosehl.curtis.platform.realtime.application.LiveEventPublisher;
import com.sosehl.curtis.platform.realtime.domain.LiveEventType;
import com.sosehl.curtis.platform.realtime.domain.LiveInvalidation;
import org.springframework.stereotype.Component;

@Component
public class RealtimeClassroomChangeNotifier
    implements ClassroomChangeNotifier {

    private final LiveEventPublisher events;

    public RealtimeClassroomChangeNotifier(LiveEventPublisher events) {
        this.events = events;
    }

    @Override
    public void rosterChanged() {
        events.publish(
            LiveInvalidation.broadcast(LiveEventType.ROSTER_CHANGED)
        );
    }
}
