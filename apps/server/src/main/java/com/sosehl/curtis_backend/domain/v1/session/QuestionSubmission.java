package com.sosehl.curtis_backend.domain.v1.session;

import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import java.util.List;
import lombok.Data;

@Data
public class QuestionSubmission {

    private QuestionType type;
    private List<Integer> selectedIndexes;
    private List<MatchingSubmissionPair> pairs;
    private String text;

    public static QuestionSubmission multipleChoice(List<Integer> indexes) {
        QuestionSubmission submission = new QuestionSubmission();
        submission.setType(QuestionType.MULTIPLE_CHOICE);
        submission.setSelectedIndexes(indexes);
        return submission;
    }
}
