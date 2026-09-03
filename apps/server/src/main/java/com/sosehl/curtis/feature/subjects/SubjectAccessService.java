package com.sosehl.curtis.feature.subjects;

import com.sosehl.curtis.feature.subjects.core.SubjectCatalog;
import com.sosehl.curtis.feature.subjects.core.SubjectSummary;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@SOSE_ReadOnlyTransaction
public class SubjectAccessService implements SubjectCatalog {

    private final SubjectRepository repository;
    private final SubjectAssignmentRepository assignments;

    SubjectAccessService(
        SubjectRepository repository,
        SubjectAssignmentRepository assignments
    ) {
        this.repository = repository;
        this.assignments = assignments;
    }

    @Override
    public SubjectSummary require(UUID subjectId) {
        return summary(requireEntity(subjectId));
    }

    @Override
    public void requireTeacherAssignment(UUID teacherId, UUID subjectId) {
        SubjectEntity subject = requireEntity(subjectId);
        if (
            !subject.active() ||
            !assignments.isAssigned(teacherId, subjectId)
        ) {
            throw ProblemException.notFound(
                "subject_not_assigned",
                "The subject is not assigned to this teacher."
            );
        }
    }

    public List<SubjectSummary> listForTeacher(UUID teacherId) {
        List<UUID> subjectIds = assignments
            .findAllByTeacherId(teacherId)
            .stream()
            .map(SubjectAssignmentEntity::subjectId)
            .toList();
        return repository
            .findByIdInAndActiveTrueOrderByNameAsc(subjectIds)
            .stream()
            .map(this::summary)
            .toList();
    }

    SubjectEntity requireEntity(UUID subjectId) {
        return repository
            .findById(subjectId)
            .orElseThrow(() ->
                ProblemException.notFound(
                    "subject_not_found",
                    "The subject does not exist."
                )
            );
    }

    SubjectSummary summary(SubjectEntity subject) {
        return new SubjectSummary(
            subject.id(),
            subject.code(),
            subject.name(),
            subject.active(),
            subject.version()
        );
    }
}
