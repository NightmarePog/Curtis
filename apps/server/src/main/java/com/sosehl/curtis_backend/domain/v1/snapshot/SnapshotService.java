package com.sosehl.curtis_backend.domain.v1.snapshot;

import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final SnapshotRepository snapshotRepository;

    public Snapshot createSnapshotIfChanged(Quiz quiz) {
        String json = SnapshotUtil.toJson(quiz);
        String hash = SnapshotUtil.hash(json);

        var lastSnapshotOpt =
            snapshotRepository.findTopByQuizOrderByCreatedAtDesc(quiz);

        if (lastSnapshotOpt.isPresent()) {
            Snapshot last = lastSnapshotOpt.get();

            if (last.getSnapshotHash().equals(hash)) {
                return last;
            }
        }

        Snapshot snapshot = new Snapshot(quiz, json, hash);
        return snapshotRepository.save(snapshot);
    }
}
