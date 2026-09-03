package com.sosehl.curtis.feature.classrooms;

import com.sosehl.curtis.feature.classrooms.core.ClassroomChangeNotifier;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.sosehl.curtis.feature.classrooms.dto.ClassroomResponse;
import com.sosehl.curtis.feature.classrooms.dto.ClassroomResponse.GroupResponse;
import com.sosehl.curtis.shared.errors.ProblemException;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.feature.users.core.UserSummary;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SOSE_ReadOnlyTransaction
public class ClassroomService {

    private final ClassroomRepository classrooms;
    private final ClassGroupRepository groups;
    private final ClassTeacherAssignmentRepository teacherAssignments;
    private final ClassStudentAssignmentRepository studentAssignments;
    private final GroupStudentAssignmentRepository groupAssignments;
    private final ClassroomAccessService access;
    private final UserDirectory users;
    private final Clock clock;
    private final ClassroomChangeNotifier changeNotifier;

    ClassroomService(
        ClassroomRepository classrooms,
        ClassGroupRepository groups,
        ClassTeacherAssignmentRepository teacherAssignments,
        ClassStudentAssignmentRepository studentAssignments,
        GroupStudentAssignmentRepository groupAssignments,
        ClassroomAccessService access,
        UserDirectory users,
        Clock clock,
        ClassroomChangeNotifier changeNotifier
    ) {
        this.classrooms = classrooms;
        this.groups = groups;
        this.teacherAssignments = teacherAssignments;
        this.studentAssignments = studentAssignments;
        this.groupAssignments = groupAssignments;
        this.access = access;
        this.users = users;
        this.clock = clock;
        this.changeNotifier = changeNotifier;
    }

    public List<ClassroomResponse> listForAdministrator() {
        return classrooms
            .findAllByOrderByNameAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<ClassroomResponse> listForTeacher(UUID teacherId) {
        List<UUID> classIds = teacherAssignments
            .findAllByTeacherId(teacherId)
            .stream()
            .map(ClassTeacherAssignmentEntity::classId)
            .toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        return classrooms
            .findAllByIdInAndActiveTrueOrderByNameAsc(classIds)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public ClassroomResponse getForAdministrator(UUID classId) {
        return toResponse(requireClass(classId));
    }

    public ClassroomResponse getForTeacher(UUID teacherId, UUID classId) {
        access.requireTeacherAssigned(teacherId, classId);
        return toResponse(requireClass(classId));
    }

    @Transactional
    public ClassroomResponse createClass(String name, UUID administratorId) {
        requireName(name);
        rejectDuplicateClassName(null, name);
        ClassroomEntity classroom = ClassroomEntity.create(
            name,
            administratorId,
            clock.instant()
        );
        return changed(toResponse(classrooms.saveAndFlush(classroom)));
    }

    @Transactional
    public ClassroomResponse updateClass(
        UUID classId,
        String name,
        Boolean active,
        long expectedVersion
    ) {
        ClassroomEntity classroom = requireClass(classId);
        requireVersion(
            classroom.version(),
            expectedVersion,
            "class_version_conflict"
        );
        if (name != null) {
            requireName(name);
            rejectDuplicateClassName(classId, name);
        }
        boolean deactivate = Boolean.FALSE.equals(active) && classroom.active();
        classroom.update(name, active, clock.instant());
        classrooms.saveAndFlush(classroom);
        if (deactivate) {
            studentAssignments.deleteAllByClassId(classId);
        }
        return changed(toResponse(classroom));
    }

    @Transactional
    public ClassroomResponse assignTeacher(
        UUID classId,
        UUID teacherId,
        UUID administratorId
    ) {
        requireActiveClass(classId);
        users.requireRole(teacherId, UserRole.TEACHER);
        if (
            !teacherAssignments.existsByClassIdAndTeacherId(
                classId,
                teacherId
            )
        ) {
            teacherAssignments.saveAndFlush(
                new ClassTeacherAssignmentEntity(
                    classId,
                    teacherId,
                    administratorId,
                    clock.instant()
                )
            );
        }
        return changed(toResponse(requireClass(classId)));
    }

    @Transactional
    public ClassroomResponse removeTeacher(UUID classId, UUID teacherId) {
        ClassroomEntity classroom = requireClass(classId);
        teacherAssignments.deleteByClassIdAndTeacherId(classId, teacherId);
        return changed(toResponse(classroom));
    }

    @Transactional
    public ClassroomResponse assignStudent(
        UUID classId,
        UUID studentId,
        UUID administratorId
    ) {
        requireActiveClass(classId);
        users.requireRole(studentId, UserRole.STUDENT);
        UUID currentClass = studentAssignments
            .findByStudentId(studentId)
            .map(ClassStudentAssignmentEntity::classId)
            .orElse(null);
        if (classId.equals(currentClass)) {
            return toResponse(requireClass(classId));
        }
        if (currentClass != null) {
            throw ProblemException.conflict(
                "student_already_assigned",
                "The student already belongs to another active class."
            );
        }
        studentAssignments.saveAndFlush(
            new ClassStudentAssignmentEntity(
                classId,
                studentId,
                administratorId,
                clock.instant()
            )
        );
        return changed(toResponse(requireClass(classId)));
    }

    @Transactional
    public ClassroomResponse removeStudent(UUID classId, UUID studentId) {
        ClassroomEntity classroom = requireClass(classId);
        studentAssignments.deleteByClassIdAndStudentId(classId, studentId);
        return changed(toResponse(classroom));
    }

    @Transactional
    public ClassroomResponse createGroup(
        UUID actorId,
        boolean administrator,
        UUID classId,
        String name
    ) {
        authorizeGroupManagement(actorId, administrator, classId);
        requireName(name);
        rejectDuplicateGroupName(null, classId, name);
        groups.save(
            ClassGroupEntity.create(
                classId,
                name,
                actorId,
                clock.instant()
            )
        );
        return changed(toResponse(requireClass(classId)));
    }

    @Transactional
    public ClassroomResponse updateGroup(
        UUID actorId,
        boolean administrator,
        UUID classId,
        UUID groupId,
        String name,
        Boolean active,
        long expectedVersion
    ) {
        authorizeGroupManagement(actorId, administrator, classId);
        ClassGroupEntity group = requireGroup(classId, groupId);
        requireVersion(
            group.version(),
            expectedVersion,
            "group_version_conflict"
        );
        if (name != null) {
            requireName(name);
            rejectDuplicateGroupName(groupId, classId, name);
        }
        boolean deactivate = Boolean.FALSE.equals(active) && group.active();
        group.update(name, active, clock.instant());
        groups.saveAndFlush(group);
        if (deactivate) {
            clearGroup(groupId);
        }
        return changed(toResponse(requireClass(classId)));
    }

    @Transactional
    public ClassroomResponse archiveGroup(
        UUID actorId,
        boolean administrator,
        UUID classId,
        UUID groupId
    ) {
        authorizeGroupManagement(actorId, administrator, classId);
        ClassGroupEntity group = requireGroup(classId, groupId);
        group.update(null, false, clock.instant());
        groups.saveAndFlush(group);
        clearGroup(groupId);
        return changed(toResponse(requireClass(classId)));
    }

    @Transactional
    public ClassroomResponse addGroupStudent(
        UUID actorId,
        boolean administrator,
        UUID classId,
        UUID groupId,
        UUID studentId
    ) {
        authorizeGroupManagement(actorId, administrator, classId);
        ClassGroupEntity group = requireGroup(classId, groupId);
        if (!group.active()) {
            throw ProblemException.conflict(
                "group_inactive",
                "Students cannot be assigned to an inactive group."
            );
        }
        if (!access.isStudentMember(studentId, classId)) {
            throw ProblemException.notFound(
                "student_not_in_class",
                "The student does not belong to this class."
            );
        }
        if (!groupAssignments.existsByGroupIdAndStudentId(groupId, studentId)) {
            groupAssignments.saveAndFlush(
                new GroupStudentAssignmentEntity(
                    groupId,
                    classId,
                    studentId,
                    actorId,
                    clock.instant()
                )
            );
        }
        return changed(toResponse(requireClass(classId)));
    }

    @Transactional
    public ClassroomResponse removeGroupStudent(
        UUID actorId,
        boolean administrator,
        UUID classId,
        UUID groupId,
        UUID studentId
    ) {
        authorizeGroupManagement(actorId, administrator, classId);
        requireGroup(classId, groupId);
        groupAssignments.deleteByGroupIdAndStudentId(groupId, studentId);
        return changed(toResponse(requireClass(classId)));
    }

    private ClassroomResponse toResponse(ClassroomEntity classroom) {
        List<UserSummary> teachers = users.summariesByIds(
            teacherAssignments
                .findAllByClassIdOrderByTeacherIdAsc(classroom.id())
                .stream()
                .map(ClassTeacherAssignmentEntity::teacherId)
                .toList()
        );
        List<UserSummary> students = users.summariesByIds(
            studentAssignments
                .findAllByClassIdOrderByStudentIdAsc(classroom.id())
                .stream()
                .map(ClassStudentAssignmentEntity::studentId)
                .toList()
        );
        List<GroupResponse> groupResponses = groups
            .findAllByClassIdOrderByNameAsc(classroom.id())
            .stream()
            .map(group ->
                new GroupResponse(
                    group.id(),
                    group.name(),
                    group.active(),
                    group.version(),
                    users.summariesByIds(
                        groupAssignments
                            .findAllByGroupIdOrderByStudentIdAsc(group.id())
                            .stream()
                            .map(GroupStudentAssignmentEntity::studentId)
                            .toList()
                    )
                )
            )
            .toList();
        return new ClassroomResponse(
            classroom.id(),
            classroom.name(),
            classroom.active(),
            classroom.version(),
            teachers,
            students,
            groupResponses
        );
    }

    private void authorizeGroupManagement(
        UUID actorId,
        boolean administrator,
        UUID classId
    ) {
        requireActiveClass(classId);
        if (!administrator) {
            access.requireTeacherAssigned(actorId, classId);
        }
    }

    private ClassroomEntity requireActiveClass(UUID classId) {
        ClassroomEntity classroom = requireClass(classId);
        if (!classroom.active()) {
            throw ProblemException.conflict(
                "class_inactive",
                "This class is inactive."
            );
        }
        return classroom;
    }

    private ClassroomEntity requireClass(UUID classId) {
        return classrooms
            .findById(classId)
            .orElseThrow(() ->
                ProblemException.notFound(
                    "class_not_found",
                    "The class does not exist."
                )
            );
    }

    private ClassGroupEntity requireGroup(UUID classId, UUID groupId) {
        return groups
            .findById(groupId)
            .filter(group -> group.classId().equals(classId))
            .orElseThrow(() ->
                ProblemException.notFound(
                    "group_not_found",
                    "The group does not exist in this class."
                )
            );
    }

    private void rejectDuplicateClassName(UUID currentId, String name) {
        classrooms
            .findByNameIgnoreCase(name.trim())
            .filter(found -> !found.id().equals(currentId))
            .ifPresent(found -> {
                throw ProblemException.conflict(
                    "class_name_exists",
                    "A class with that name already exists."
                );
            });
    }

    private void rejectDuplicateGroupName(
        UUID currentId,
        UUID classId,
        String name
    ) {
        groups
            .findByClassIdAndNameIgnoreCase(classId, name.trim())
            .filter(found -> !found.id().equals(currentId))
            .ifPresent(found -> {
                throw ProblemException.conflict(
                    "group_name_exists",
                    "A group with that name already exists in this class."
                );
            });
    }

    private void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw ProblemException.badRequest(
                "name_required",
                "A non-blank name is required."
            );
        }
    }

    private void requireVersion(
        long actual,
        long expected,
        String code
    ) {
        if (actual != expected) {
            throw ProblemException.conflict(
                code,
                "The resource changed since it was loaded."
            );
        }
    }

    private void clearGroup(UUID groupId) {
        groupAssignments.deleteAllByGroupId(groupId);
    }

    private ClassroomResponse changed(ClassroomResponse response) {
        changeNotifier.rosterChanged();
        return response;
    }
}
