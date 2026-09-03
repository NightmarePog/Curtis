package com.sosehl.curtis.feature.sessions;

import com.fasterxml.jackson.databind.JsonNode;
import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import com.sosehl.curtis.feature.sessions.core.SessionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "sessions")
public class QuizSession {

    @Id
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "title_snapshot", nullable = false)
    private String title;

    @Column(name = "description_snapshot", columnDefinition = "text")
    private String description;

    @Column(name = "subject_snapshot", length = 120)
    private String subject;

    @Column(name = "chapter_snapshot", length = 100)
    private String chapter;

    @Column(name = "teacher_name_snapshot", nullable = false)
    private String teacherName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quiz_snapshot", nullable = false, columnDefinition = "jsonb")
    private JsonNode quizSnapshot;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "closes_at", nullable = false)
    private Instant closesAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_policy", nullable = false, length = 20)
    private ScorePolicy scorePolicy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_classes", joinColumns = @JoinColumn(name = "session_id"))
    private Set<SessionClassTarget> classTargets = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_groups", joinColumns = @JoinColumn(name = "session_id"))
    private Set<SessionGroupTarget> groupTargets = new LinkedHashSet<>();

    @Version
    private long version;

    protected QuizSession() {}

    public QuizSession(UUID id, UUID quizId, UUID teacherId, String title,
                       String description, String subject, String chapter,
                       String teacherName, JsonNode quizSnapshot, Instant startedAt,
                       Instant closesAt, int maxAttempts, ScorePolicy scorePolicy) {
        this.id = id;
        this.quizId = quizId;
        this.teacherId = teacherId;
        this.status = SessionStatus.ACTIVE;
        this.title = title;
        this.description = description;
        this.subject = subject;
        this.chapter = chapter;
        this.teacherName = teacherName;
        this.quizSnapshot = quizSnapshot;
        this.startedAt = startedAt;
        this.closesAt = closesAt;
        this.maxAttempts = maxAttempts;
        this.scorePolicy = scorePolicy;
    }

    public UUID getId() { return id; }
    public UUID getQuizId() { return quizId; }
    public UUID getTeacherId() { return teacherId; }
    public SessionStatus getStatus() { return status; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getSubject() { return subject; }
    public String getChapter() { return chapter; }
    public String getTeacherName() { return teacherName; }
    public JsonNode getQuizSnapshot() { return quizSnapshot; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getClosesAt() { return closesAt; }
    public int getMaxAttempts() { return maxAttempts; }
    public ScorePolicy getScorePolicy() { return scorePolicy; }
    public Set<SessionClassTarget> getClassTargets() { return classTargets; }
    public Set<SessionGroupTarget> getGroupTargets() { return groupTargets; }
    public boolean isActiveAt(Instant now) {
        return status == SessionStatus.ACTIVE && now.isBefore(closesAt);
    }

    public void close(SessionStatus terminalStatus, Instant now) {
        if (status == SessionStatus.ACTIVE) {
            status = terminalStatus;
            closedAt = now;
        }
    }
}
