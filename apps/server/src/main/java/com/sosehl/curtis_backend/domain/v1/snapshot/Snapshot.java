package com.sosehl.curtis_backend.domain.v1.snapshot;

import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import lombok.Data;

@Data
public class Snapshot {

    Quiz quiz;
    Integer expiresInMinutes;
    SnapshotStatus status = SnapshotStatus.SCHEDULED;
}
