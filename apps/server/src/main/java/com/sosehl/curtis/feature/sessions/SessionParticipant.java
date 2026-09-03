package com.sosehl.curtis.feature.sessions;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "session_participants")
public class SessionParticipant {
    @EmbeddedId
    private SessionParticipantId id;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    protected SessionParticipant() {}

    public SessionParticipant(UUID sessionId, UUID studentId, UUID classId) {
        this.id = new SessionParticipantId(sessionId, studentId);
        this.classId = classId;
    }

    public SessionParticipantId getId() { return id; }
    public UUID getClassId() { return classId; }
}
