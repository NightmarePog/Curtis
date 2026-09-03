package com.sosehl.curtis.feature.quizzes;

import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "questions")
public class QuestionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private QuizEntity quiz;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type;

    @Column(nullable = false, length = 2000)
    private String prompt;

    @Column(nullable = false)
    private int points;

    @Column(name = "code_snippet", length = 20000)
    private String codeSnippet;

    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "time_seconds", nullable = false)
    private int timeSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 100)
    private List<QuestionOptionEntity> options = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 100)
    private List<MatchingPairEntity> pairs = new ArrayList<>();

    protected QuestionEntity() {}

    public QuestionEntity(UUID id, int position, Instant now) {
        this.id = id;
        this.position = position;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void attachTo(QuizEntity value) { quiz = value; }
    public UUID getId() { return id; }
    public int getPosition() { return position; }
    public QuestionType getType() { return type; }
    public void setType(QuestionType value) { type = value; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String value) { prompt = value; }
    public int getPoints() { return points; }
    public void setPoints(int value) { points = value; }
    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String value) { codeSnippet = value; }
    public UUID getMediaId() { return mediaId; }
    public void setMediaId(UUID value) { mediaId = value; }
    public int getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(int value) { timeSeconds = value; }
    public List<QuestionOptionEntity> getOptions() { return options; }
    public List<MatchingPairEntity> getPairs() { return pairs; }
    public void replaceOptions(List<QuestionOptionEntity> replacements) {
        options.clear();
        for (QuestionOptionEntity option : replacements) {
            option.attachTo(this);
            options.add(option);
        }
    }
    public void replacePairs(List<MatchingPairEntity> replacements) {
        pairs.clear();
        for (MatchingPairEntity pair : replacements) {
            pair.attachTo(this);
            pairs.add(pair);
        }
    }
}
