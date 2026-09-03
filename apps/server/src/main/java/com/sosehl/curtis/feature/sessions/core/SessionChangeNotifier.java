package com.sosehl.curtis.feature.sessions.core;

import java.util.Set;
import java.util.UUID;

public interface SessionChangeNotifier {

    void sessionsChanged(Set<UUID> recipients);

    void resultsChanged(UUID studentId, UUID teacherId);
}
