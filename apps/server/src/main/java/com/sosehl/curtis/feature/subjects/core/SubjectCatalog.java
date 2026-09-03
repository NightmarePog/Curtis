package com.sosehl.curtis.feature.subjects.core;

import java.util.UUID;

public interface SubjectCatalog {

    SubjectSummary require(UUID subjectId);

    void requireTeacherAssignment(UUID teacherId, UUID subjectId);
}
