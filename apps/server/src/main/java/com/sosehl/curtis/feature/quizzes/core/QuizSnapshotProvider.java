package com.sosehl.curtis.feature.quizzes.core;

import java.util.UUID;

public interface QuizSnapshotProvider {
    QuizSnapshot loadLaunchable(UUID quizId, UUID teacherUserId);
}
