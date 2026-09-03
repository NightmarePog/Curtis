package com.sosehl.curtis.feature.sessions;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionParticipantGroupRepository
    extends JpaRepository<SessionParticipantGroup, SessionParticipantGroupId> {

    List<SessionParticipantGroup> findById_GroupIdAndId_SessionIdIn(
        UUID groupId,
        Collection<UUID> sessionIds
    );
}
