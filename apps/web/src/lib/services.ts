import { DEMO_MODE } from "@/lib/constants";
import { dump as dumpYaml, load as loadYaml } from "js-yaml";
import {
  demoActiveSessions, demoAdminClasses, demoAdminCreateClass,
  demoAdminCreateSubject, demoAdminDeleteClass, demoAdminDeleteSubject,
  demoAdminGetClass, demoAdminGetSubject, demoAdminSubjects, demoAdminTeachers,
  demoAdminStudents,
  demoAdminUpdateClass, demoAdminUpdateSubject, demoAssignedSubjects,
  demoAvailableQuizzes, demoClassLeaderboards, demoCreateClass,
  demoCreateGroup, demoCreateQuestion, demoCreateQuiz, demoCreateSession,
  demoDeleteClass,
  demoDeleteQuestion, demoDeleteQuiz, demoFinishSession, demoGetClass,
  demoGetQuiz, demoGradeAnswer, demoImportQuiz, demoJoinSession,
  demoListClasses, demoListQuestions, demoListQuizzes, demoLogin, demoLogout,
  demoMe, demoMyResults, demoNextQuestion, demoPatchQuestion, demoPatchQuiz,
  demoPendingAnswers, demoSessionResults, demoSetGroupStudent,
  demoTeacherSessionHistory, demoTeacherStudents, demoUpdateClass,
} from "@/lib/demo-store";
import { ApiError, request } from "@/lib/http";
import type {
  ActiveSession, AdminClassInput, AdminClassResponse, AdminSubjectInput,
  AdminSubjectResponse, AdminTeacherResponse, ApiAdminUser, ApiAttempt,
  ApiAttemptDetail, ApiAttemptSummary, ApiClassroom, ApiMe, ApiQuestionResult,
  ApiQuiz, ApiQuizQuestion, ApiQuizStatus, ApiQuizWrite, ApiRanking, ApiReview,
  ApiSession, ApiStudentQuestion, ApiSubject, ApiTeacherClassStudents,
  AssignedSubject, ClassLeaderboard, ClassInput, ClassResponse, FinishResult,
  Me, Page, PendingTextAnswer, Question, QuestionInput, QuestionSubmission,
  Quiz, QuizInput, QuizResult, StoredQuestionResult, TeacherAttemptSummary,
  TeacherStudentSummary,
} from "@/types/domain";

const emptyTimestamp = "1970-01-01T00:00:00.000Z";

function presentationMe(value: ApiMe): Me {
  return {
    sub: value.id, name: value.displayName, roles: value.roles, id: value.id,
    subject: value.subject, username: value.username, displayName: value.displayName,
  };
}

function classMember(user: ApiClassroom["students"][number]) {
  return { studentId: user.id, studentName: user.displayName, preferredUsername: user.username };
}

function presentationClass(value: ApiClassroom): ClassResponse {
  return {
    uuid: value.id, name: value.name, studentCount: value.students.length,
    members: value.students.map(classMember), createdAt: emptyTimestamp,
    updatedAt: emptyTimestamp, active: value.active, version: value.version,
    groups: value.groups.map((group) => ({
      uuid: group.id, name: group.name, active: group.active,
      version: group.version, members: group.students.map(classMember),
    })),
  };
}

function teacherSummary(user: ApiClassroom["teachers"][number]) {
  return {
    teacherId: user.id, displayName: user.displayName,
    preferredUsername: user.username || null,
  };
}

function presentationAdminClass(value: ApiClassroom): AdminClassResponse {
  return {
    ...presentationClass(value), teacherCount: value.teachers.length,
    teachers: value.teachers.map(teacherSummary),
  };
}

function presentationSubject(value: ApiSubject): AssignedSubject {
  return { uuid: value.id, name: value.name };
}

function presentationAdminSubject(value: ApiSubject): AdminSubjectResponse {
  const teachers = (value.teachers ?? []).map(teacherSummary);
  return {
    uuid: value.id, name: value.name, code: value.code, active: value.active,
    version: value.version, teacherCount: teachers.length, teachers,
    createdAt: emptyTimestamp, updatedAt: emptyTimestamp,
  };
}

function presentationQuestion(value: ApiQuizQuestion, quizId: string): Question {
  return {
    id: value.id, backendId: value.id, question: value.prompt, type: value.type,
    points: value.points, codeSnippet: value.codeSnippet, imageRef: value.mediaId,
    answers: value.options.map((option) => ({
      answer: option.text, isCorrect: option.correct, backendId: option.id,
    })),
    pairs: value.pairs.map((pair) => ({
      left: pair.left, right: pair.right, backendId: pair.id,
    })),
    timeInSeconds: value.timeSeconds, quizUuid: quizId,
  };
}

function presentationQuiz(value: ApiQuiz): Quiz {
  return {
    uuid: value.id, version: value.version, creatorId: value.creatorId,
    title: value.title, description: value.description, subject: value.subjectName,
    subjectUuid: value.subjectId, chapter: value.chapter,
    questions: value.questions.map((question) => presentationQuestion(question, value.id)),
    maxQuestionsPerSession: value.maxQuestionsPerSession, shuffle: value.shuffle,
    status: value.status === "PUBLISHED" ? "RUNNING" : value.status,
    createdAt: value.createdAt, editedAt: value.updatedAt,
    validFrom: value.validFrom, validTo: value.validTo,
  };
}

function apiQuizStatus(status: Quiz["status"] | undefined): ApiQuizStatus {
  return status === "RUNNING" ? "PUBLISHED" : (status ?? "DRAFT");
}

function instantOrNull(value: string | null | undefined) {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : parsed.toISOString();
}

function uuidOrNull(value: string | number | null | undefined) {
  return typeof value === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
    ? value : null;
}

function writeQuestion(
  input: QuestionInput,
  id?: string | number,
): ApiQuizWrite["questions"][number] {
  return {
    id: uuidOrNull(id), type: input.type, prompt: input.question,
    points: input.points, codeSnippet: input.codeSnippet?.trim() || null,
    mediaId: uuidOrNull(input.imageRef), timeSeconds: input.timeInSeconds,
    options: (input.answers ?? []).map((answer) => ({
      id: uuidOrNull(answer.backendId), text: answer.answer ?? "",
      correct: answer.isCorrect === true,
    })),
    pairs: (input.pairs ?? []).map((pair) => ({
      id: uuidOrNull(pair.backendId), left: pair.left, right: pair.right,
    })),
  };
}

function writeFromApi(value: ApiQuiz): ApiQuizWrite {
  return {
    title: value.title, description: value.description, subjectId: value.subjectId,
    chapter: value.chapter, status: value.status,
    maxQuestionsPerSession: value.maxQuestionsPerSession, shuffle: value.shuffle,
    validFrom: value.validFrom, validTo: value.validTo,
    expectedVersion: value.version,
    questions: value.questions.map((question) => ({
      id: question.id, type: question.type, prompt: question.prompt,
      points: question.points, codeSnippet: question.codeSnippet,
      mediaId: question.mediaId, timeSeconds: question.timeSeconds,
      options: question.options.map((option) => ({
        id: option.id, text: option.text, correct: option.correct,
      })),
      pairs: question.pairs.map((pair) => ({
        id: pair.id, left: pair.left, right: pair.right,
      })),
    })),
  };
}

function page<T>(content: T[], number: number, size: number): Page<T> {
  const start = number * size;
  const pageContent = content.slice(start, start + size);
  const totalPages = Math.ceil(content.length / size);
  return {
    content: pageContent, totalElements: content.length, totalPages, number, size,
    first: number === 0, last: number >= Math.max(0, totalPages - 1),
  };
}

function presentationSession(value: ApiSession): ActiveSession {
  return {
    sessionUuid: value.id, quizUuid: value.quizId, title: value.title,
    description: value.description, subject: value.subject, chapter: value.chapter,
    teacherName: value.teacherName, questionCount: value.questionCount,
    startedAt: value.startedAt, expiresAt: value.closesAt,
    status: value.status,
    openToAllStudents: false,
    assignedClasses: value.targets.map((target) => ({
      uuid: target.id,
      name: target.type === "GROUP" ? `Skupina ${target.name}` : target.name,
    })),
  };
}

function secondsUntil(deadline: string | null) {
  if (!deadline) return 0;
  return Math.max(1, Math.ceil((new Date(deadline).getTime() - Date.now()) / 1000));
}

function presentationStudentQuestion(value: ApiStudentQuestion, sessionId: string): Question {
  return {
    id: value.id, backendId: value.id, question: value.prompt, type: value.type,
    points: value.points, codeSnippet: value.codeSnippet, imageRef: value.mediaId,
    answers: value.options.map((option) => ({
      answer: option.text, isCorrect: null, backendId: option.id,
    })),
    pairs: value.leftItems.map((left, index) => ({
      left: left.text, right: value.rightItems[index]?.text ?? "",
      leftId: left.id, rightId: value.rightItems[index]?.id,
    })),
    timeInSeconds: secondsUntil(value.deadlineAt), quizUuid: sessionId,
  };
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown> : {};
}

function stringArray(value: unknown) {
  return Array.isArray(value)
    ? value.filter((item): item is string => typeof item === "string") : [];
}

function presentationQuestionResult(
  value: ApiQuestionResult,
  definition?: ApiQuizQuestion,
): StoredQuestionResult {
  const response = asRecord(value.response);
  const optionIds = stringArray(response.optionIds);
  const submittedPairs = Array.isArray(response.pairs) ? response.pairs.map(asRecord) : [];
  return {
    id: value.id, questionIndex: value.position, question: value.prompt,
    type: value.type, points: value.maxPoints, awardedPoints: value.awardedPoints,
    status: value.gradingState === "PENDING_REVIEW" ? "PENDING_REVIEW" : "GRADED",
    text: typeof response.text === "string" ? response.text : null,
    selectedIndexes: optionIds.map((id, fallbackIndex) => {
      const snapshotIndex = value.options.findIndex((option) => option.id === id);
      const index = snapshotIndex >= 0
        ? snapshotIndex
        : (definition?.options.findIndex((option) => option.id === id) ?? -1);
      return index >= 0 ? index : fallbackIndex;
    }),
    pairs: submittedPairs.map((pair, fallbackIndex) => {
      const leftId = typeof pair.leftId === "string" ? pair.leftId : "";
      const rightId = typeof pair.rightId === "string" ? pair.rightId : "";
      const snapshotLeftIndex = value.matchingPairs.findIndex((item) => item.leftId === leftId);
      const leftIndex = snapshotLeftIndex >= 0
        ? snapshotLeftIndex
        : (definition?.pairs.findIndex((item) => item.id === leftId) ?? -1);
      const rightIndex = value.matchingPairs.findIndex((item) => item.rightId === rightId);
      return {
        leftIndex: leftIndex >= 0 ? leftIndex : fallbackIndex,
        rightIndex: rightIndex >= 0 ? rightIndex : fallbackIndex,
      };
    }),
    optionLabels: value.options.map((option) => option.text),
    matchingPairLabels: value.matchingPairs.map((pair) => ({
      left: pair.left,
      right: pair.right,
    })),
  };
}

function presentationResult(detail: ApiAttemptDetail, quiz?: ApiQuiz): QuizResult {
  return {
    id: detail.attempt.id, sessionUuid: detail.attempt.sessionId,
    quizUuid: quiz?.id ?? detail.attempt.sessionId,
    quizTitle: detail.attempt.sessionTitle, studentId: detail.attempt.studentId,
    studentName: detail.attempt.studentName, score: detail.attempt.score,
    maxScore: detail.attempt.maxScore,
    playedAt: detail.attempt.submittedAt ?? detail.attempt.startedAt,
    questionResults: detail.questions.map((question) => {
      const definition = quiz?.questions.find((candidate) => candidate.prompt === question.prompt);
      return presentationQuestionResult(question, definition);
    }),
  };
}

function presentationAttempt(value: ApiAttemptSummary): TeacherAttemptSummary {
  return {
    resultId: value.id, sessionUuid: value.sessionId, quizUuid: value.sessionId,
    quizTitle: value.sessionTitle, studentId: value.studentId,
    studentName: value.studentName, score: value.score, maxScore: value.maxScore,
    percentage: value.percentage, playedAt: value.submittedAt ?? value.startedAt,
    questionCount: 0, pendingReviewCount: value.pendingReviewCount,
  };
}

function presentationReview(value: ApiReview): PendingTextAnswer {
  return {
    resultId: value.questionResultId, studentId: value.studentId,
    studentName: value.studentName, questionIndex: 0, question: value.prompt,
    text: value.answer, points: value.maxPoints, awardedPoints: value.awardedPoints,
    status: "PENDING_REVIEW",
  };
}

function unsupportedTeacherClassMutation(): never {
  throw new ApiError(
    405,
    "Class names and rosters are managed by an administrator. Teachers can manage groups only.",
    { code: "class.administrator_managed" },
  );
}

export const authService = {
  me: async (): Promise<Me> => DEMO_MODE
    ? demoMe() : presentationMe(await request<ApiMe>("/v1/me")),
  demoLogin: async (role: "ADMINISTRATOR" | "TEACHER" | "STUDENT") => demoLogin(role),
  logoutLocal: async () => {
    if (DEMO_MODE) {
      demoLogout();
      return;
    }
    await request<void>("/logout", { method: "POST" });
  },
};

async function teacherQuiz(quizId: string) {
  return request<ApiQuiz>(`/v1/teacher/quizzes/${quizId}`);
}

function mutableRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : null;
}

async function yamlForApi(file: File, images: File[]) {
  const source = await file.text();
  const document = mutableRecord(loadYaml(source));
  if (!document) return source;

  document.schemaVersion ??= 1;
  if (document.status === "RUNNING") document.status = "PUBLISHED";

  if (!document.subjectId && typeof document.subject === "string") {
    const subjects = await request<ApiSubject[]>("/v1/teacher/subjects");
    const subject = subjects.find(
      (candidate) => candidate.name.localeCompare(document.subject as string, "cs", {
        sensitivity: "accent",
      }) === 0,
    );
    if (!subject) {
      throw new ApiError(400, "The YAML subject is not assigned to this teacher.", {
        code: "quiz.subject_not_assigned",
      });
    }
    document.subjectId = subject.id;
  }
  delete document.subject;

  const uploaded = new Map<string, string>();
  for (const image of images) {
    const form = new FormData();
    form.append("file", image);
    const media = await request<{ id: string }>("/v1/teacher/media", {
      method: "POST",
      body: form,
    });
    uploaded.set(image.name, media.id);
  }

  if (Array.isArray(document.questions)) {
    for (const rawQuestion of document.questions) {
      const question = mutableRecord(rawQuestion);
      if (!question) continue;
      const imageKey = typeof question.image === "string"
        ? question.image
        : typeof question.imageRef === "string"
          ? question.imageRef
          : null;
      if (imageKey && uploaded.has(imageKey)) {
        question.image = uploaded.get(imageKey);
        delete question.imageRef;
      }
      if (Array.isArray(question.options) &&
        question.options.every((option) => typeof option === "string")) {
        const correct = new Set(
          Array.isArray(question.correctIndexes)
            ? question.correctIndexes.filter((index): index is number => Number.isInteger(index))
            : [],
        );
        question.options = question.options.map((option, index) => ({
          text: option,
          correct: correct.has(index),
        }));
      }
      delete question.correctIndexes;
    }
  }
  return dumpYaml(document, { lineWidth: 120, noRefs: true });
}

export const quizService = {
  list: async (number = 0, size = 20): Promise<Page<Quiz>> => {
    if (DEMO_MODE) return demoListQuizzes(number, size);
    const values = (await request<ApiQuiz[]>("/v1/teacher/quizzes")).map(presentationQuiz);
    return page(values, number, size);
  },
  available: async (): Promise<Quiz[]> => {
    if (DEMO_MODE) return demoAvailableQuizzes();
    const now = Date.now();
    return (await request<ApiQuiz[]>("/v1/teacher/quizzes"))
      .filter((quiz) => quiz.status === "PUBLISHED" &&
        (!quiz.validFrom || new Date(quiz.validFrom).getTime() <= now) &&
        (!quiz.validTo || new Date(quiz.validTo).getTime() >= now))
      .map(presentationQuiz);
  },
  get: async (quizId: string): Promise<Quiz> => DEMO_MODE
    ? demoGetQuiz(quizId) : presentationQuiz(await teacherQuiz(quizId)),
  create: async (input: QuizInput): Promise<{ quizUuid: string }> => {
    if (DEMO_MODE) return demoCreateQuiz(input);
    if (!input.subjectUuid) {
      throw new ApiError(400, "A subject must be selected.", { code: "quiz.subject_required" });
    }
    const created = await request<{ quizId: string; version: number }>("/v1/teacher/quizzes", {
      method: "POST",
      body: JSON.stringify({
        title: input.title, description: input.description?.trim() || null,
        subjectId: input.subjectUuid, chapter: input.chapter?.trim() || null,
        status: apiQuizStatus(input.status),
        maxQuestionsPerSession: input.maxQuestionsPerSession,
        shuffle: input.shuffle ?? false, validFrom: instantOrNull(input.validFrom),
        validTo: instantOrNull(input.validTo), expectedVersion: null, questions: [],
      } satisfies ApiQuizWrite),
    });
    return { quizUuid: created.quizId };
  },
  update: async (quizId: string, input: Partial<QuizInput>): Promise<void> => {
    if (DEMO_MODE) return demoPatchQuiz(quizId, input);
    const current = await teacherQuiz(quizId);
    const write = writeFromApi(current);
    write.title = input.title ?? write.title;
    write.description = input.description === undefined ? write.description : input.description.trim() || null;
    write.subjectId = input.subjectUuid ?? write.subjectId;
    write.chapter = input.chapter === undefined ? write.chapter : input.chapter.trim() || null;
    write.status = input.status === undefined ? write.status : apiQuizStatus(input.status);
    write.maxQuestionsPerSession = input.maxQuestionsPerSession ?? write.maxQuestionsPerSession;
    write.shuffle = input.shuffle ?? write.shuffle;
    write.validFrom = input.validFrom === undefined ? write.validFrom : instantOrNull(input.validFrom);
    write.validTo = input.validTo === undefined ? write.validTo : instantOrNull(input.validTo);
    await request<ApiQuiz>(`/v1/teacher/quizzes/${quizId}`, {
      method: "PUT", body: JSON.stringify(write),
    });
  },
  remove: async (quizId: string): Promise<void> => {
    if (DEMO_MODE) return demoDeleteQuiz(quizId);
    const current = await teacherQuiz(quizId);
    await request<void>(`/v1/teacher/quizzes/${quizId}?expectedVersion=${current.version}`, { method: "DELETE" });
  },
  importYaml: async (file: File, images: File[] = []): Promise<{ quizUuid: string }> => {
    if (DEMO_MODE) return demoImportQuiz(file);
    const created = await request<{ quizId: string; version: number }>("/v1/teacher/quizzes/yaml", {
      method: "POST",
      headers: { "Content-Type": "application/yaml" },
      body: await yamlForApi(file, images),
    });
    return { quizUuid: created.quizId };
  },
  questions: async (quizId: string): Promise<Question[]> => DEMO_MODE
    ? demoListQuestions(quizId)
    : (await teacherQuiz(quizId)).questions.map((question) => presentationQuestion(question, quizId)),
  addQuestion: async (quizId: string, input: QuestionInput): Promise<void> => {
    if (DEMO_MODE) return demoCreateQuestion(quizId, input);
    const current = await teacherQuiz(quizId);
    const write = writeFromApi(current);
    write.questions.push(writeQuestion(input));
    await request<ApiQuiz>(`/v1/teacher/quizzes/${quizId}`, { method: "PUT", body: JSON.stringify(write) });
  },
  updateQuestion: async (
    quizId: string, questionId: string | number, input: Partial<QuestionInput>,
  ): Promise<void> => {
    if (DEMO_MODE) {
      if (typeof questionId !== "number") throw new Error("Invalid demo question identifier.");
      return demoPatchQuestion(quizId, questionId, input);
    }
    const current = await teacherQuiz(quizId);
    const index = current.questions.findIndex((question) => question.id === String(questionId));
    if (index < 0) throw new ApiError(404, "The question was not found.", { code: "question.not_found" });
    const previous = presentationQuestion(current.questions[index], quizId);
    const merged: QuestionInput = {
      question: input.question ?? previous.question, type: input.type ?? previous.type,
      points: input.points ?? previous.points,
      timeInSeconds: input.timeInSeconds ?? previous.timeInSeconds,
      codeSnippet: input.codeSnippet ?? previous.codeSnippet ?? undefined,
      imageRef: input.imageRef ?? previous.imageRef ?? undefined,
      answers: input.answers ?? previous.answers, pairs: input.pairs ?? previous.pairs,
    };
    const write = writeFromApi(current);
    write.questions[index] = writeQuestion(merged, current.questions[index].id);
    await request<ApiQuiz>(`/v1/teacher/quizzes/${quizId}`, { method: "PUT", body: JSON.stringify(write) });
  },
  removeQuestion: async (quizId: string, questionId: string | number): Promise<void> => {
    if (DEMO_MODE) {
      if (typeof questionId !== "number") throw new Error("Invalid demo question identifier.");
      return demoDeleteQuestion(quizId, questionId);
    }
    const current = await teacherQuiz(quizId);
    const write = writeFromApi(current);
    write.questions = write.questions.filter((question) => question.id !== String(questionId));
    await request<ApiQuiz>(`/v1/teacher/quizzes/${quizId}`, { method: "PUT", body: JSON.stringify(write) });
  },
};

export const classService = {
  list: async (): Promise<ClassResponse[]> => DEMO_MODE
    ? demoListClasses()
    : (await request<ApiClassroom[]>("/v1/teacher/classes")).map(presentationClass),
  get: async (classId: string): Promise<ClassResponse> => DEMO_MODE
    ? demoGetClass(classId)
    : presentationClass(await request<ApiClassroom>(`/v1/teacher/classes/${classId}`)),
  create: async (input: ClassInput): Promise<ClassResponse> => DEMO_MODE
    ? demoCreateClass(input) : unsupportedTeacherClassMutation(),
  update: async (classId: string, input: Partial<ClassInput>): Promise<ClassResponse> => DEMO_MODE
    ? demoUpdateClass(classId, input) : unsupportedTeacherClassMutation(),
  remove: async (classId: string): Promise<void> => {
    if (DEMO_MODE) return demoDeleteClass(classId);
    void classId;
    unsupportedTeacherClassMutation();
  },
  createGroup: async (classId: string, name: string): Promise<ClassResponse> => {
    if (DEMO_MODE) return demoCreateGroup(classId, name);
    return presentationClass(await request<ApiClassroom>(
      `/v1/teacher/classes/${classId}/groups`,
      { method: "POST", body: JSON.stringify({ name }) },
    ));
  },
  setGroupStudent: async (
    classId: string,
    groupId: string,
    studentId: string,
    assigned: boolean,
  ): Promise<ClassResponse> => {
    if (DEMO_MODE) {
      return demoSetGroupStudent(classId, groupId, studentId, assigned);
    }
    return presentationClass(await request<ApiClassroom>(
      `/v1/teacher/classes/${classId}/groups/${groupId}/students/${studentId}`,
      { method: assigned ? "PUT" : "DELETE" },
    ));
  },
};

export const subjectService = {
  list: async (): Promise<AssignedSubject[]> => DEMO_MODE
    ? demoAssignedSubjects()
    : (await request<ApiSubject[]>("/v1/teacher/subjects")).map(presentationSubject),
};

async function updateClassAssignments(
  classId: string, current: ApiClassroom, teacherIds?: string[], studentIds?: string[],
) {
  let updated = current;
  if (teacherIds) {
    const desired = new Set(teacherIds);
    for (const teacher of current.teachers) {
      if (!desired.has(teacher.id)) {
        updated = await request<ApiClassroom>(`/v1/admin/classes/${classId}/teachers/${teacher.id}`, { method: "DELETE" });
      }
    }
    const existing = new Set(current.teachers.map((teacher) => teacher.id));
    for (const teacherId of desired) {
      if (!existing.has(teacherId)) {
        updated = await request<ApiClassroom>(`/v1/admin/classes/${classId}/teachers/${teacherId}`, { method: "PUT" });
      }
    }
  }
  if (studentIds) {
    const desired = new Set(studentIds);
    for (const student of current.students) {
      if (!desired.has(student.id)) {
        updated = await request<ApiClassroom>(`/v1/admin/classes/${classId}/students/${student.id}`, { method: "DELETE" });
      }
    }
    const existing = new Set(current.students.map((student) => student.id));
    for (const studentId of desired) {
      if (!existing.has(studentId)) {
        updated = await request<ApiClassroom>(`/v1/admin/classes/${classId}/students/${studentId}`, { method: "PUT" });
      }
    }
  }
  return updated;
}

function subjectCode(name: string) {
  const value = name.normalize("NFD").replace(/[\u0300-\u036f]/g, "")
    .toUpperCase().replace(/[^A-Z0-9]+/g, "_").replace(/^_|_$/g, "").slice(0, 32);
  return value || "SUBJECT";
}

async function updateSubjectAssignments(
  subjectId: string, current: ApiSubject, teacherIds?: string[],
) {
  let updated = current;
  if (!teacherIds) return updated;
  const desired = new Set(teacherIds);
  const currentTeachers = current.teachers ?? [];
  for (const teacher of currentTeachers) {
    if (!desired.has(teacher.id)) {
      updated = await request<ApiSubject>(`/v1/admin/subjects/${subjectId}/teachers/${teacher.id}`, { method: "DELETE" });
    }
  }
  const existing = new Set(currentTeachers.map((teacher) => teacher.id));
  for (const teacherId of desired) {
    if (!existing.has(teacherId)) {
      updated = await request<ApiSubject>(`/v1/admin/subjects/${subjectId}/teachers/${teacherId}`, { method: "PUT" });
    }
  }
  return updated;
}

export const adminService = {
  teachers: async (): Promise<AdminTeacherResponse[]> => {
    if (DEMO_MODE) return demoAdminTeachers();
    return (await request<ApiAdminUser[]>("/v1/admin/users?role=TEACHER&active=true"))
      .map((teacher) => ({
        teacherId: teacher.id, displayName: teacher.displayName,
        preferredUsername: teacher.username || null, lastSeenAt: teacher.lastLoginAt,
      }));
  },
  students: async (): Promise<ClassResponse["members"]> => {
    if (DEMO_MODE) return demoAdminStudents();
    return (await request<ApiAdminUser[]>("/v1/admin/users?role=STUDENT&active=true"))
      .map((student) => ({
        studentId: student.id,
        studentName: student.displayName,
        preferredUsername: student.username || null,
      }));
  },
  classes: async (): Promise<AdminClassResponse[]> => DEMO_MODE
    ? demoAdminClasses()
    : (await request<ApiClassroom[]>("/v1/admin/classes")).map(presentationAdminClass),
  getClass: async (classId: string): Promise<AdminClassResponse> => DEMO_MODE
    ? demoAdminGetClass(classId)
    : presentationAdminClass(await request<ApiClassroom>(`/v1/admin/classes/${classId}`)),
  createClass: async (input: AdminClassInput): Promise<AdminClassResponse> => {
    if (DEMO_MODE) return demoAdminCreateClass(input);
    const created = await request<ApiClassroom>("/v1/admin/classes", {
      method: "POST", body: JSON.stringify({ name: input.name }),
    });
    return presentationAdminClass(
      await updateClassAssignments(created.id, created, input.teacherIds, input.studentIds),
    );
  },
  updateClass: async (
    classId: string, input: Partial<AdminClassInput>,
  ): Promise<AdminClassResponse> => {
    if (DEMO_MODE) return demoAdminUpdateClass(classId, input);
    let current = await request<ApiClassroom>(`/v1/admin/classes/${classId}`);
    if (input.name !== undefined) {
      current = await request<ApiClassroom>(`/v1/admin/classes/${classId}`, {
        method: "PATCH", body: JSON.stringify({ name: input.name, version: current.version }),
      });
    }
    current = await updateClassAssignments(classId, current, input.teacherIds, input.studentIds);
    return presentationAdminClass(current);
  },
  removeClass: async (classId: string): Promise<void> => {
    if (DEMO_MODE) return demoAdminDeleteClass(classId);
    const current = await request<ApiClassroom>(`/v1/admin/classes/${classId}`);
    await request<ApiClassroom>(`/v1/admin/classes/${classId}`, {
      method: "PATCH", body: JSON.stringify({ active: false, version: current.version }),
    });
  },
  subjects: async (): Promise<AdminSubjectResponse[]> => DEMO_MODE
    ? demoAdminSubjects()
    : (await request<ApiSubject[]>("/v1/admin/subjects")).map(presentationAdminSubject),
  getSubject: async (subjectId: string): Promise<AdminSubjectResponse> => {
    if (DEMO_MODE) return demoAdminGetSubject(subjectId);
    const value = (await request<ApiSubject[]>("/v1/admin/subjects"))
      .find((subject) => subject.id === subjectId);
    if (!value) throw new ApiError(404, "The subject was not found.", { code: "subject.not_found" });
    return presentationAdminSubject(value);
  },
  createSubject: async (input: AdminSubjectInput): Promise<AdminSubjectResponse> => {
    if (DEMO_MODE) return demoAdminCreateSubject(input);
    const created = await request<ApiSubject>("/v1/admin/subjects", {
      method: "POST", body: JSON.stringify({ code: subjectCode(input.name), name: input.name }),
    });
    return presentationAdminSubject(
      await updateSubjectAssignments(created.id, created, input.teacherIds),
    );
  },
  updateSubject: async (
    subjectId: string, input: Partial<AdminSubjectInput>,
  ): Promise<AdminSubjectResponse> => {
    if (DEMO_MODE) return demoAdminUpdateSubject(subjectId, input);
    let current = (await request<ApiSubject[]>("/v1/admin/subjects"))
      .find((subject) => subject.id === subjectId);
    if (!current) throw new ApiError(404, "The subject was not found.", { code: "subject.not_found" });
    if (input.name !== undefined) {
      current = await request<ApiSubject>(`/v1/admin/subjects/${subjectId}`, {
        method: "PATCH", body: JSON.stringify({ name: input.name, version: current.version }),
      });
    }
    current = await updateSubjectAssignments(subjectId, current, input.teacherIds);
    return presentationAdminSubject(current);
  },
  removeSubject: async (subjectId: string): Promise<void> => {
    if (DEMO_MODE) return demoAdminDeleteSubject(subjectId);
    const current = (await request<ApiSubject[]>("/v1/admin/subjects"))
      .find((subject) => subject.id === subjectId);
    if (!current) throw new ApiError(404, "The subject was not found.", { code: "subject.not_found" });
    await request<ApiSubject>(`/v1/admin/subjects/${subjectId}`, {
      method: "PATCH", body: JSON.stringify({ active: false, version: current.version }),
    });
  },
};

const attemptsBySession = new Map<string, ApiAttempt>();

async function detailForStudent(attemptId: string) {
  return request<ApiAttemptDetail>(`/v1/student/results/${attemptId}`);
}

export const sessionService = {
  active: async (): Promise<ActiveSession[]> => DEMO_MODE
    ? demoActiveSessions()
    : (await request<ApiSession[]>("/v1/student/sessions")).map(presentationSession),
  teacherActive: async (): Promise<ActiveSession[]> => {
    if (DEMO_MODE) return demoActiveSessions();
    return (await request<ApiSession[]>("/v1/teacher/sessions"))
      .filter((session) => session.status === "ACTIVE").map(presentationSession);
  },
  teacherSessions: async (): Promise<ActiveSession[]> => {
    if (DEMO_MODE) return demoActiveSessions();
    return (await request<ApiSession[]>("/v1/teacher/sessions")).map(presentationSession);
  },
  create: async (
    quizId: string, expiresInMinutes: number, classIds: string[] = [], groupIds: string[] = [],
  ): Promise<string> => {
    if (DEMO_MODE) {
      return demoCreateSession(
        quizId,
        expiresInMinutes,
        classIds,
        groupIds,
      );
    }
    const created = await request<ApiSession>("/v1/teacher/sessions", {
      method: "POST",
      body: JSON.stringify({
        quizId, classIds, groupIds,
        closesAt: new Date(Date.now() + expiresInMinutes * 60_000).toISOString(),
        maxAttempts: 1, scorePolicy: "BEST",
      }),
    });
    return created.id;
  },
  join: async (sessionId: string): Promise<Question | null> => {
    if (DEMO_MODE) return demoJoinSession(sessionId);
    const attempt = await request<ApiAttempt>(`/v1/student/sessions/${sessionId}/attempts`, { method: "POST" });
    attemptsBySession.set(sessionId, attempt);
    if (!attempt.currentQuestion) return null;
    return presentationStudentQuestion(attempt.currentQuestion, sessionId);
  },
  next: async (sessionId: string, submission: QuestionSubmission): Promise<Question> => {
    if (DEMO_MODE) return demoNextQuestion(sessionId, submission);
    let attempt = attemptsBySession.get(sessionId);
    if (!attempt) {
      throw new ApiError(409, "The attempt is not active in this browser.", { code: "attempt.not_active" });
    }
    const question = attempt.currentQuestion;
    if (!question) {
      throw new ApiError(409, "The attempt is ready to submit.", { code: "attempt_ready_to_submit" });
    }
    const answer = submission.type === "MULTIPLE_CHOICE"
      ? {
          type: submission.type,
          optionIds: submission.selectedIndexes
            .map((index) => question.options[index]?.id).filter(Boolean),
          pairs: [], text: null,
        }
      : submission.type === "MATCHING"
        ? {
            type: submission.type, optionIds: [],
            pairs: submission.pairs.map((pair) => ({
              leftId: question.leftItems[pair.leftIndex]?.id,
              rightId: question.rightItems[pair.rightIndex]?.id,
            })), text: null,
          }
        : { type: submission.type, optionIds: [], pairs: [], text: submission.text };
    attempt = await request<ApiAttempt>(
      `/v1/student/attempts/${attempt.id}/questions/${question.id}/answer`,
      { method: "PUT", body: JSON.stringify(answer) },
    );
    attemptsBySession.set(sessionId, attempt);
    if (!attempt.currentQuestion) {
      throw new ApiError(409, "The attempt is ready to submit.", { code: "attempt_ready_to_submit" });
    }
    return presentationStudentQuestion(attempt.currentQuestion, sessionId);
  },
  finish: async (sessionId: string): Promise<FinishResult> => {
    if (DEMO_MODE) return demoFinishSession(sessionId);
    const current = attemptsBySession.get(sessionId);
    if (!current) {
      throw new ApiError(409, "The attempt is not active in this browser.", { code: "attempt.not_active" });
    }
    const attempt = await request<ApiAttempt>(`/v1/student/attempts/${current.id}/submit`, { method: "POST" });
    attemptsBySession.set(sessionId, attempt);
    return {
      score: attempt.score,
      maxScore: attempt.maxScore,
      pendingReviewCount: attempt.pendingReviewCount,
      questions: [],
    };
  },
  myResults: async (): Promise<QuizResult[]> => {
    if (DEMO_MODE) return demoMyResults();
    const summaries = await request<ApiAttemptSummary[]>("/v1/student/results?limit=100");
    const details = await Promise.all(summaries.map((attempt) => detailForStudent(attempt.id)));
    return details.map((detail) => presentationResult(detail));
  },
  myLeaderboards: async (): Promise<ClassLeaderboard[]> => {
    if (DEMO_MODE) return demoClassLeaderboards();
    return (await request<ApiRanking[]>("/v1/student/rankings")).map((ranking) => ({
      classUuid: ranking.id, className: ranking.name,
      members: ranking.members.map((member) => ({
        studentName: member.displayName, totalScore: member.score,
        totalMaxScore: member.maxScore, percentage: member.percentage,
        attemptCount: member.attemptCount, rank: member.rank,
        currentStudent: member.currentStudent,
      })),
    }));
  },
  history: async (limit = 100): Promise<TeacherAttemptSummary[]> => DEMO_MODE
    ? demoTeacherSessionHistory(limit)
    : (await request<ApiAttemptSummary[]>(
        `/v1/teacher/sessions/history?limit=${encodeURIComponent(String(limit))}`,
      )).map(presentationAttempt),
  students: async (limit = 100, attemptLimit = 20): Promise<TeacherStudentSummary[]> => {
    if (DEMO_MODE) return demoTeacherStudents(limit, attemptLimit);
    const classes = await request<ApiTeacherClassStudents[]>("/v1/teacher/students");
    const unique = new Map<string, ApiTeacherClassStudents["students"][number]>();
    for (const schoolClass of classes) {
      for (const student of schoolClass.students) unique.set(student.id, student);
    }
    return Promise.all([...unique.values()].slice(0, limit).map(async (student) => {
      const profile = await request<{ attempts: ApiAttemptSummary[] }>(`/v1/teacher/students/${student.id}`);
      const attempts = profile.attempts.slice(0, attemptLimit).map(presentationAttempt);
      return {
        studentId: student.id, studentName: student.displayName,
        attemptCount: student.attemptCount, totalScore: student.score,
        totalMaxScore: student.maxScore, percentage: student.percentage,
        lastPlayedAt: student.lastActivity ?? attempts[0]?.playedAt ?? emptyTimestamp,
        attempts,
      };
    }));
  },
  results: async (sessionId: string): Promise<QuizResult[]> => {
    if (DEMO_MODE) return demoSessionResults(sessionId);
    const [summaries, sessions] = await Promise.all([
      request<ApiAttemptSummary[]>(`/v1/teacher/sessions/${sessionId}/attempts`),
      request<ApiSession[]>("/v1/teacher/sessions"),
    ]);
    const session = sessions.find((value) => value.id === sessionId);
    const quiz = session ? await teacherQuiz(session.quizId).catch(() => undefined) : undefined;
    const details = await Promise.all(
      summaries.map((attempt) => request<ApiAttemptDetail>(`/v1/teacher/attempts/${attempt.id}`)),
    );
    return details.map((detail) => presentationResult(detail, quiz));
  },
  pending: async (sessionId: string): Promise<PendingTextAnswer[]> => {
    if (DEMO_MODE) return demoPendingAnswers(sessionId);
    return (await request<ApiReview[]>("/v1/teacher/reviews"))
      .filter((review) => review.sessionId === sessionId).map(presentationReview);
  },
  grade: async (
    sessionId: string, resultId: string | number, awardedPoints: number,
  ): Promise<PendingTextAnswer> => {
    if (DEMO_MODE) {
      if (typeof resultId !== "number") throw new Error("Invalid demo result identifier.");
      return demoGradeAnswer(sessionId, resultId, awardedPoints);
    }
    await request<ApiQuestionResult>(`/v1/teacher/reviews/${resultId}`, {
      method: "PUT", body: JSON.stringify({ points: awardedPoints }),
    });
    return {
      resultId, studentId: "", studentName: null, questionIndex: 0,
      question: "", text: "", points: awardedPoints, awardedPoints,
      status: "GRADED",
    };
  },
};
