package com.sosehl.curtis.feature.quizzes;

import com.sosehl.curtis.feature.quizzes.core.QuizStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "quizzes")
public class QuizEntity {

    @Id
    private UUID id;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(length = 100)
    private String chapter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizStatus status;

    @Column(name = "max_questions_per_session", nullable = false)
    private int maxQuestionsPerSession;

    @Column(nullable = false)
    private boolean shuffle;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private long version;

    @OneToMany(
        mappedBy = "quiz",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @OrderBy("position ASC")
    @BatchSize(size = 50)
    private List<QuestionEntity> questions = new ArrayList<>();

    protected QuizEntity() {}

    public QuizEntity(UUID id, UUID creatorId, Instant now) {
        this.id = id;
        this.creatorId = creatorId;
        this.createdAt = now;
        this.updatedAt = now;
        this.status = QuizStatus.DRAFT;
    }

    public UUID getId() { return id; }
    public UUID getCreatorId() { return creatorId; }
    public UUID getSubjectId() { return subjectId; }
    public void setSubjectId(UUID value) { subjectId = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getChapter() { return chapter; }
    public void setChapter(String value) { chapter = value; }
    public QuizStatus getStatus() { return status; }
    public void setStatus(QuizStatus value) { status = value; }
    public int getMaxQuestionsPerSession() { return maxQuestionsPerSession; }
    public void setMaxQuestionsPerSession(int value) { maxQuestionsPerSession = value; }
    public boolean isShuffle() { return shuffle; }
    public void setShuffle(boolean value) { shuffle = value; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant value) { validFrom = value; }
    public Instant getValidTo() { return validTo; }
    public void setValidTo(Instant value) { validTo = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
    public Instant getArchivedAt() { return archivedAt; }
    public void archive(Instant now) {
        status = QuizStatus.ARCHIVED;
        archivedAt = now;
        updatedAt = now;
    }
    public long getVersion() { return version; }
    public List<QuestionEntity> getQuestions() { return questions; }
    public void replaceQuestions(List<QuestionEntity> replacements) {
        questions.clear();
        for (QuestionEntity question : replacements) {
            question.attachTo(this);
            questions.add(question);
        }
    }
}
