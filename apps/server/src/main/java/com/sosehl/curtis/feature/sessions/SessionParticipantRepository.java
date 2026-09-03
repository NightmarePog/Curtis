package com.sosehl.curtis.feature.sessions;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionParticipantRepository extends JpaRepository<SessionParticipant, SessionParticipantId> {
    List<SessionParticipant> findByIdSessionId(UUID sessionId);

    List<SessionParticipant> findByIdStudentId(UUID studentId);

    List<SessionParticipant> findByClassIdAndId_SessionIdIn(
        UUID classId,
        Collection<UUID> sessionIds
    );
}
