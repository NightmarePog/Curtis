package com.sosehl.curtis.feature.classrooms;

import com.sosehl.curtis.feature.classrooms.core.AudienceStudent;
import com.sosehl.curtis.feature.classrooms.core.ClassSummary;
import com.sosehl.curtis.feature.classrooms.core.ClassroomAudienceResolver;
import com.sosehl.curtis.feature.classrooms.core.ClassroomDirectory;
import com.sosehl.curtis.feature.classrooms.core.ClassroomRoster;
import com.sosehl.curtis.feature.classrooms.core.GroupRoster;
import com.sosehl.curtis.feature.classrooms.core.GroupSummary;
import com.sosehl.curtis.feature.classrooms.core.ResolvedAudience;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
@SOSE_ReadOnlyTransaction
public class ClassroomAccessService
    implements ClassroomAudienceResolver, ClassroomDirectory {

    private final ClassroomRepository classrooms;
    private final ClassGroupRepository groups;
    private final ClassTeacherAssignmentRepository teacherAssignments;
    private final ClassStudentAssignmentRepository studentAssignments;
    private final GroupStudentAssignmentRepository groupAssignments;
    private final UserDirectory users;

    public ClassroomAccessService(
        ClassroomRepository classrooms,
        ClassGroupRepository groups,
        ClassTeacherAssignmentRepository teacherAssignments,
        ClassStudentAssignmentRepository studentAssignments,
        GroupStudentAssignmentRepository groupAssignments,
        UserDirectory users
    ) {
        this.classrooms = classrooms;
        this.groups = groups;
        this.teacherAssignments = teacherAssignments;
        this.studentAssignments = studentAssignments;
        this.groupAssignments = groupAssignments;
        this.users = users;
    }

    @Override
    public ResolvedAudience resolveAudience(
        UUID teacherId,
        Set<UUID> classIds,
        Set<UUID> groupIds
    ) {
        Set<UUID> requestedClasses = safeSet(classIds);
        Set<UUID> requestedGroups = safeSet(groupIds);
        if (requestedClasses.isEmpty() && requestedGroups.isEmpty()) {
            throw ProblemException.badRequest(
                "audience_required",
                "At least one class or group must be selected."
            );
        }

        List<ClassSummary> classes = loadClasses(requestedClasses);
        List<GroupSummary> selectedGroups = loadGroups(requestedGroups);
        Set<UUID> allClassIds = new LinkedHashSet<>(requestedClasses);
        selectedGroups.forEach(group -> allClassIds.add(group.classId()));
        requireTeacherAssignedToAll(teacherId, allClassIds);

        selectedGroups
            .stream()
            .filter(group -> requestedClasses.contains(group.classId()))
            .findFirst()
            .ifPresent(group -> {
                throw ProblemException.badRequest(
                    "redundant_audience_target",
                    "A whole class and one of its groups cannot both be selected."
                );
            });

        Set<UUID> activeStudents = users.activeIdsWithRole(UserRole.STUDENT);
        Map<UUID, MutableAudienceStudent> students = new LinkedHashMap<>();
        loadClassStudents(requestedClasses, activeStudents).forEach(row ->
            students.putIfAbsent(
                row.studentId(),
                new MutableAudienceStudent(row.studentId(), row.classId())
            )
        );
        loadGroupStudents(requestedGroups, activeStudents).forEach(row -> {
            MutableAudienceStudent student = students.computeIfAbsent(
                row.studentId(),
                ignored ->
                    new MutableAudienceStudent(row.studentId(), row.classId())
            );
            student.groupIds.add(row.groupId());
        });

        if (students.isEmpty()) {
            throw ProblemException.badRequest(
                "audience_empty",
                "The selected audience has no active students."
            );
        }

        List<AudienceStudent> audienceStudents = students
            .values()
            .stream()
            .map(MutableAudienceStudent::snapshot)
            .sorted(Comparator.comparing(AudienceStudent::studentId))
            .toList();
        return new ResolvedAudience(classes, selectedGroups, audienceStudents);
    }

    @Override
    public List<ClassroomRoster> activeForTeacher(UUID teacherId) {
        List<UUID> classIds = teacherAssignments
            .findAllByTeacherId(teacherId)
            .stream()
            .map(ClassTeacherAssignmentEntity::classId)
            .toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        List<ClassroomEntity> values = classrooms
            .findAllByIdInAndActiveTrueOrderByNameAsc(classIds);
        return toRosters(
            values,
            users.activeIdsWithRole(UserRole.STUDENT)
        );
    }

    @Override
    public Optional<ClassroomRoster> currentForStudent(UUID studentId) {
        return studentAssignments
            .findByStudentId(studentId)
            .flatMap(assignment -> classrooms.findById(assignment.classId()))
            .map(classroom ->
                toRosters(
                    List.of(classroom),
                    users.activeIdsWithRole(UserRole.STUDENT)
                ).get(0)
            );
    }

    @Override
    public boolean isTeacherAssigned(UUID teacherId, UUID classId) {
        return (
            users.activeIdsWithRole(UserRole.TEACHER).contains(teacherId) &&
            classrooms.existsByIdAndActiveTrue(classId) &&
            teacherAssignments.existsByClassIdAndTeacherId(
                classId,
                teacherId
            )
        );
    }

    public void requireTeacherAssigned(UUID teacherId, UUID classId) {
        if (!isTeacherAssigned(teacherId, classId)) {
            throw classNotAssigned();
        }
    }

    public boolean isStudentMember(UUID studentId, UUID classId) {
        return studentAssignments.existsByClassIdAndStudentId(
            classId,
            studentId
        );
    }

    private List<ClassSummary> loadClasses(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<ClassSummary> values = classrooms
            .findAllByIdInAndActiveTrueOrderByNameAsc(ids)
            .stream()
            .map(classroom ->
                new ClassSummary(classroom.id(), classroom.name())
            )
            .toList();
        if (values.size() != ids.size()) {
            throw ProblemException.notFound(
                "class_not_found",
                "One or more selected classes do not exist."
            );
        }
        return values;
    }

    private List<GroupSummary> loadGroups(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<ClassGroupEntity> values = groups
            .findAllByIdInAndActiveTrueOrderByNameAsc(ids);
        Set<UUID> activeClassIds = activeClassIds(
            values.stream().map(ClassGroupEntity::classId).toList()
        );
        List<GroupSummary> summaries = values
            .stream()
            .filter(group -> activeClassIds.contains(group.classId()))
            .map(group ->
                new GroupSummary(group.id(), group.classId(), group.name())
            )
            .toList();
        if (summaries.size() != ids.size()) {
            throw ProblemException.notFound(
                "group_not_found",
                "One or more selected groups do not exist."
            );
        }
        return summaries;
    }

    private List<StudentClassRow> loadClassStudents(
        Set<UUID> classIds,
        Set<UUID> activeStudents
    ) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        return studentAssignments
            .findAllByClassIdIn(classIds)
            .stream()
            .filter(row -> activeStudents.contains(row.studentId()))
            .map(row ->
                new StudentClassRow(row.studentId(), row.classId())
            )
            .toList();
    }

    private List<StudentGroupRow> loadGroupStudents(
        Set<UUID> groupIds,
        Set<UUID> activeStudents
    ) {
        if (groupIds.isEmpty()) {
            return List.of();
        }
        return groupAssignments
            .findAllByGroupIdIn(groupIds)
            .stream()
            .filter(row -> activeStudents.contains(row.studentId()))
            .map(row ->
                new StudentGroupRow(
                    row.studentId(),
                    row.classId(),
                    row.groupId()
                )
            )
            .toList();
    }

    private List<ClassroomRoster> toRosters(
        List<ClassroomEntity> classroomValues,
        Set<UUID> activeStudents
    ) {
        if (classroomValues.isEmpty()) {
            return List.of();
        }
        Set<UUID> classIds = classroomValues
            .stream()
            .map(ClassroomEntity::id)
            .collect(Collectors.toUnmodifiableSet());

        Map<UUID, List<UUID>> studentsByClass = new LinkedHashMap<>();
        studentAssignments
            .findAllByClassIdIn(classIds)
            .stream()
            .filter(assignment ->
                activeStudents.contains(assignment.studentId())
            )
            .forEach(assignment ->
                studentsByClass
                    .computeIfAbsent(
                        assignment.classId(),
                        ignored -> new ArrayList<>()
                    )
                    .add(assignment.studentId())
            );
        studentsByClass
            .values()
            .forEach(ids -> ids.sort(Comparator.naturalOrder()));

        List<ClassGroupEntity> groupValues = groups
            .findAllByClassIdInAndActiveTrueOrderByNameAsc(classIds);
        Set<UUID> groupIds = groupValues
            .stream()
            .map(ClassGroupEntity::id)
            .collect(Collectors.toUnmodifiableSet());
        Map<UUID, List<UUID>> studentsByGroup = new LinkedHashMap<>();
        if (!groupIds.isEmpty()) {
            groupAssignments
                .findAllByGroupIdIn(groupIds)
                .stream()
                .filter(assignment ->
                    activeStudents.contains(assignment.studentId())
                )
                .forEach(assignment ->
                    studentsByGroup
                        .computeIfAbsent(
                            assignment.groupId(),
                            ignored -> new ArrayList<>()
                        )
                        .add(assignment.studentId())
                );
        }
        studentsByGroup
            .values()
            .forEach(ids -> ids.sort(Comparator.naturalOrder()));

        Map<UUID, List<GroupRoster>> groupsByClass = new LinkedHashMap<>();
        groupValues.forEach(group ->
            groupsByClass
                .computeIfAbsent(
                    group.classId(),
                    ignored -> new ArrayList<>()
                )
                .add(
                    new GroupRoster(
                        group.id(),
                        group.classId(),
                        group.name(),
                        studentsByGroup.getOrDefault(group.id(), List.of())
                    )
                )
        );

        return classroomValues
            .stream()
            .map(classroom ->
                new ClassroomRoster(
                    classroom.id(),
                    classroom.name(),
                    classroom.active(),
                    studentsByClass.getOrDefault(
                        classroom.id(),
                        List.of()
                    ),
                    groupsByClass.getOrDefault(
                        classroom.id(),
                        List.of()
                    )
                )
            )
            .toList();
    }

    private Set<UUID> activeClassIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        return classrooms
            .findAllByIdInAndActiveTrueOrderByNameAsc(ids)
            .stream()
            .map(ClassroomEntity::id)
            .collect(Collectors.toUnmodifiableSet());
    }

    private void requireTeacherAssignedToAll(
        UUID teacherId,
        Set<UUID> classIds
    ) {
        if (!users.activeIdsWithRole(UserRole.TEACHER).contains(teacherId)) {
            throw classNotAssigned();
        }
        Set<UUID> assignedClassIds = teacherAssignments
            .findAllByTeacherIdAndClassIdIn(teacherId, classIds)
            .stream()
            .map(ClassTeacherAssignmentEntity::classId)
            .collect(Collectors.toUnmodifiableSet());
        if (!assignedClassIds.containsAll(classIds)) {
            throw classNotAssigned();
        }
    }

    private ProblemException classNotAssigned() {
        return ProblemException.notFound(
            "class_not_found",
            "The class is not assigned to this teacher."
        );
    }

    private Set<UUID> safeSet(Set<UUID> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    private record StudentClassRow(UUID studentId, UUID classId) {}

    private record StudentGroupRow(
        UUID studentId,
        UUID classId,
        UUID groupId
    ) {}

    private static final class MutableAudienceStudent {

        private final UUID studentId;
        private final UUID classId;
        private final Set<UUID> groupIds = new LinkedHashSet<>();

        private MutableAudienceStudent(UUID studentId, UUID classId) {
            this.studentId = studentId;
            this.classId = classId;
        }

        private AudienceStudent snapshot() {
            return new AudienceStudent(studentId, classId, groupIds);
        }
    }
}
