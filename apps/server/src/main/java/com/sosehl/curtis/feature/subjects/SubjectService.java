package com.sosehl.curtis.feature.subjects;

import com.sosehl.curtis.feature.subjects.dto.SubjectResponse;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.sosehl.curtis.shared.errors.ProblemException;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.feature.users.core.UserSummary;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SOSE_ReadOnlyTransaction
public class SubjectService {

    private final SubjectRepository repository;
    private final SubjectAssignmentRepository assignments;
    private final SubjectAccessService accessService;
    private final UserDirectory userDirectory;
    private final Clock clock;

    SubjectService(
        SubjectRepository repository,
        SubjectAssignmentRepository assignments,
        SubjectAccessService accessService,
        UserDirectory userDirectory,
        Clock clock
    ) {
        this.repository = repository;
        this.assignments = assignments;
        this.accessService = accessService;
        this.userDirectory = userDirectory;
        this.clock = clock;
    }

    public List<SubjectResponse> list() {
        return repository
            .findAllByOrderByNameAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SubjectResponse create(String code, String name) {
        rejectDuplicate(null, code, name);
        SubjectEntity subject = SubjectEntity.create(
            code,
            name,
            clock.instant()
        );
        return toResponse(repository.saveAndFlush(subject));
    }

    @Transactional
    public SubjectResponse update(
        UUID subjectId,
        String code,
        String name,
        Boolean active,
        long expectedVersion
    ) {
        SubjectEntity subject = accessService.requireEntity(subjectId);
        if (subject.version() != expectedVersion) {
            throw ProblemException.conflict(
                "subject_version_conflict",
                "The subject changed since it was loaded."
            );
        }
        rejectDuplicate(subjectId, code, name);
        subject.update(code, name, active, clock.instant());
        return toResponse(repository.saveAndFlush(subject));
    }

    @Transactional
    public SubjectResponse assignTeacher(
        UUID subjectId,
        UUID teacherId,
        UUID administratorId
    ) {
        SubjectEntity subject = accessService.requireEntity(subjectId);
        if (!subject.active()) {
            throw ProblemException.conflict(
                "subject_inactive",
                "Teachers cannot be assigned to an inactive subject."
            );
        }
        userDirectory.requireRole(teacherId, UserRole.TEACHER);
        SubjectAssignmentId id = new SubjectAssignmentId(
            teacherId,
            subjectId
        );
        if (!assignments.existsById(id)) {
            assignments.save(
                new SubjectAssignmentEntity(
                    teacherId,
                    subjectId,
                    administratorId,
                    clock.instant()
                )
            );
        }
        return toResponse(subject);
    }

    @Transactional
    public SubjectResponse unassignTeacher(UUID subjectId, UUID teacherId) {
        SubjectEntity subject = accessService.requireEntity(subjectId);
        assignments.deleteById(new SubjectAssignmentId(teacherId, subjectId));
        return toResponse(subject);
    }

    private void rejectDuplicate(UUID currentId, String code, String name) {
        if (code != null) {
            repository
                .findByCodeIgnoreCase(code.trim().toUpperCase(Locale.ROOT))
                .filter(found -> !found.id().equals(currentId))
                .ifPresent(found -> {
                    throw ProblemException.conflict(
                        "subject_code_exists",
                        "A subject with that code already exists."
                    );
                });
        }
        if (name != null) {
            repository
                .findByNameIgnoreCase(name.trim())
                .filter(found -> !found.id().equals(currentId))
                .ifPresent(found -> {
                    throw ProblemException.conflict(
                        "subject_name_exists",
                        "A subject with that name already exists."
                    );
                });
        }
    }

    private SubjectResponse toResponse(SubjectEntity subject) {
        List<UUID> teacherIds = assignments
            .findAllBySubjectId(subject.id())
            .stream()
            .map(SubjectAssignmentEntity::teacherId)
            .toList();
        List<UserSummary> teachers = userDirectory.summariesByIds(teacherIds);
        return new SubjectResponse(
            subject.id(),
            subject.code(),
            subject.name(),
            subject.active(),
            subject.version(),
            teachers
        );
    }

}
