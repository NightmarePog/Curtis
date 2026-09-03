package com.sosehl.curtis.feature.sessions;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "session_participant_groups")
public class SessionParticipantGroup {
    @EmbeddedId
    private SessionParticipantGroupId id;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    protected SessionParticipantGroup() {}

    public SessionParticipantGroup(
        UUID sessionId,
        UUID studentId,
        UUID groupId,
        UUID classId
    ) {
        this.id = new SessionParticipantGroupId(sessionId, studentId, groupId);
        this.classId = classId;
    }

    public SessionParticipantGroupId getId() { return id; }
    public UUID getClassId() { return classId; }
}
