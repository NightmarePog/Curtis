package com.sosehl.curtis.feature.subjects;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sosehl.curtis.shared.errors.ProblemException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubjectAccessServiceTest {

    @Mock
    private SubjectRepository subjects;

    @Mock
    private SubjectAssignmentRepository assignments;

    private SubjectAccessService service;

    @BeforeEach
    void setUp() {
        service = new SubjectAccessService(subjects, assignments);
    }

    @Test
    void unassignedSubjectIsHiddenFromTeacher() {
        UUID teacherId = UUID.randomUUID();
        SubjectEntity subject = SubjectEntity.create(
            "MAT",
            "Mathematics",
            Instant.parse("2026-09-02T10:00:00Z")
        );
        when(subjects.findById(subject.id())).thenReturn(Optional.of(subject));
        when(assignments.isAssigned(teacherId, subject.id())).thenReturn(false);

        assertThatThrownBy(() ->
            service.requireTeacherAssignment(teacherId, subject.id())
        )
            .isInstanceOf(ProblemException.class)
            .satisfies(exception ->
                org.assertj.core.api.Assertions.assertThat(
                    ((ProblemException) exception).code()
                ).isEqualTo("subject_not_assigned")
            );
    }
}
