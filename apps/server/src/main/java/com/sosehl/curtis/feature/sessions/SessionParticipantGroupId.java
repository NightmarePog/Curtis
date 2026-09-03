package com.sosehl.curtis.feature.sessions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SessionParticipantGroupId implements Serializable {
    @Column(name = "session_id")
    private UUID sessionId;
    @Column(name = "student_id")
    private UUID studentId;
    @Column(name = "group_id")
    private UUID groupId;

    protected SessionParticipantGroupId() {}

    public SessionParticipantGroupId(UUID sessionId, UUID studentId, UUID groupId) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.groupId = groupId;
    }

    public UUID getSessionId() { return sessionId; }
    public UUID getStudentId() { return studentId; }
    public UUID getGroupId() { return groupId; }

    @Override
    public boolean equals(Object other) {
        return other instanceof SessionParticipantGroupId value
            && Objects.equals(sessionId, value.sessionId)
            && Objects.equals(studentId, value.studentId)
            && Objects.equals(groupId, value.groupId);
    }

    @Override
    public int hashCode() { return Objects.hash(sessionId, studentId, groupId); }
}
