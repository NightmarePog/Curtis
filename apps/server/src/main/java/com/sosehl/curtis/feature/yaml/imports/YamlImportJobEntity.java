package com.sosehl.curtis.feature.yaml.imports;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "yaml_import_jobs")
public class YamlImportJobEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "source_path", nullable = false, length = 1000)
    private String sourcePath;

    @Column(name = "content_digest", nullable = false, unique = true, length = 64)
    private String contentDigest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private YamlImportJobStatus status;

    @Column(name = "quiz_id")
    private UUID quizId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_details", columnDefinition = "jsonb")
    private JsonNode errorDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    private long version;

    protected YamlImportJobEntity() {}

    public YamlImportJobEntity(
        UUID id,
        UUID ownerId,
        String sourcePath,
        String contentDigest,
        Instant now
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.sourcePath = sourcePath;
        this.contentDigest = contentDigest;
        this.status = YamlImportJobStatus.PROCESSING;
        this.createdAt = now;
    }

    public void complete(UUID quizId, Instant now) {
        this.quizId = quizId;
        this.status = YamlImportJobStatus.COMPLETED;
        this.processedAt = now;
    }

    public void fail(JsonNode error, Instant now) {
        this.status = YamlImportJobStatus.FAILED;
        this.errorDetails = error;
        this.processedAt = now;
    }

    public YamlImportJobStatus getStatus() { return status; }
    public UUID getQuizId() { return quizId; }
    public JsonNode getErrorDetails() { return errorDetails; }
}
