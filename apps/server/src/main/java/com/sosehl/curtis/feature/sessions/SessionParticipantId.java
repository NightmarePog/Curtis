package com.sosehl.curtis.feature.sessions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SessionParticipantId implements Serializable {
    @Column(name = "session_id")
    private UUID sessionId;
    @Column(name = "student_id")
    private UUID studentId;

    protected SessionParticipantId() {}

    public SessionParticipantId(UUID sessionId, UUID studentId) {
        this.sessionId = sessionId;
        this.studentId = studentId;
    }

    public UUID getSessionId() { return sessionId; }
    public UUID getStudentId() { return studentId; }

    @Override
    public boolean equals(Object other) {
        return other instanceof SessionParticipantId value
            && Objects.equals(sessionId, value.sessionId)
            && Objects.equals(studentId, value.studentId);
    }

    @Override
    public int hashCode() { return Objects.hash(sessionId, studentId); }
}
