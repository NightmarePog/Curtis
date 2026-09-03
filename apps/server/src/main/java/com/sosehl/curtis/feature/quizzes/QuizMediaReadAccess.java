package com.sosehl.curtis.feature.quizzes;

import com.sosehl.curtis.feature.media.core.MediaAccessContributor;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
@SOSE_ReadOnlyTransaction
public class QuizMediaReadAccess implements MediaAccessContributor {

    private final QuizRepository quizzes;

    public QuizMediaReadAccess(QuizRepository quizzes) {
        this.quizzes = quizzes;
    }

    @Override
    public boolean canRead(UUID userId, UUID mediaId) {
        return quizzes.existsByCreatorIdAndQuestions_MediaId(
            userId,
            mediaId
        );
    }
}
