import { faker } from "@faker-js/faker/locale/cs_CZ";
import { load as loadYaml } from "js-yaml";

import { ApiError } from "@/lib/http";
import { DEMO_DATABASE_CHANGED_EVENT } from "@/lib/live-events";
import type {
  ActiveSession,
  AdminClassInput,
  AdminClassResponse,
  AdminSubjectInput,
  AdminSubjectResponse,
  AdminTeacherResponse,
  AssignedSubject,
  ClassMember,
  ClassInput,
  ClassLeaderboard,
  ClassResponse,
  FinishResult,
  Me,
  Page,
  PendingTextAnswer,
  Question,
  QuestionInput,
  QuestionSubmission,
  Quiz,
  QuizInput,
  QuizResult,
  TeacherAttemptSummary,
  TeacherStudentSummary,
} from "@/types/domain";

const DATABASE_KEY = "curtis.fresh-demo.database.v8";
const ROLE_KEY = "curtis.fresh-demo.role.v1";
const ATTEMPT_KEY = "curtis.fresh-demo.attempt.v1";
const DEMO_FAKER_SEED = 20_260_902;

type DemoRole = "ADMINISTRATOR" | "TEACHER" | "STUDENT";

interface DemoAttempt {
  sessionUuid: string;
  questionIndex: number;
  submissions: QuestionSubmission[];
}

interface DemoSession extends ActiveSession {
  /** Teacher who launched the session. */
  teacherId: string;
  /** Audience snapshot captured when the demo session starts. */
  eligibleStudentIds: string[] | null;
}

interface DemoResult extends QuizResult {
  /** Teacher who owns the source session and its grading workflow. */
  teacherId: string;
  /** Class audience snapshot copied from the session when the result is stored. */
  assignedClassUuids: string[];
}

interface DemoClass extends ClassResponse {
  teacherIds: string[];
}

interface DemoSubject extends AssignedSubject {
  teacherIds: string[];
  createdAt: string;
  updatedAt: string;
}

interface DemoDatabase {
  administrator: { id: string; name: string };
  teacher: { id: string; name: string };
  teachers: AdminTeacherResponse[];
  students: ClassMember[];
  classes: DemoClass[];
  subjects: DemoSubject[];
  quizzes: Quiz[];
  sessions: DemoSession[];
  results: DemoResult[];
  pending: PendingTextAnswer[];
  nextQuestionId: number;
  nextResultId: number;
}

function iso(minutesFromNow = 0) {
  return new Date(Date.now() + minutesFromNow * 60_000).toISOString();
}

function demoProblem(status: number, code: string, detail: string) {
  const title = new Map([
    [400, "Bad Request"],
    [401, "Unauthorized"],
    [403, "Forbidden"],
    [404, "Not Found"],
    [409, "Conflict"],
    [410, "Gone"],
  ]).get(status) ?? "Error";
  return new ApiError(status, detail, {
    type: `urn:curtis:error:${code}`,
    title,
    status,
    detail,
    code,
    traceId: "demo",
  });
}

function seedDatabase(): DemoDatabase {
  faker.seed(DEMO_FAKER_SEED);

  const fullName = (sex: "female" | "male") =>
    `${faker.person.firstName(sex)} ${faker.person.lastName(sex)}`;
  const networkQuizUuid = "1d4b9a12-5fe5-4eb3-9c1c-2bf145a23210";
  const networkSessionUuid = "aafbd64a-5f0e-48aa-bf36-523c1eebd5d4";
  const technologyClassUuid = "80a9708a-c51e-4a8d-9ea8-653eb5e6ce11";
  const electricalClassUuid = "57e5d73d-c8dc-46ac-8ee1-519a4ea2dff7";
  const technologyGroupAUuid = "6beb26b8-f797-4daa-b433-cdad2fdb254b";
  const technologyGroupBUuid = "a8b2f715-3f0b-4394-95db-c37202dd3830";
  const networkSubjectUuid = "125c00f8-eb46-447f-b9ed-4921483e1ab2";
  const electricalSubjectUuid = "52482974-43a1-46b5-8c63-b73bf44a52b9";
  const teacher = {
    id: "demo-teacher",
    name: `Ing. ${fullName("female")}`,
  };
  const secondTeacher: AdminTeacherResponse = {
    teacherId: "demo-teacher-electro",
    displayName: `Mgr. ${fullName("male")}`,
    preferredUsername: "vyucujici.elektro@sosehl.cz",
    lastSeenAt: iso(-95),
  };
  const teachers: AdminTeacherResponse[] = [
    {
      teacherId: teacher.id,
      displayName: teacher.name,
      preferredUsername: "vyucujici.site@sosehl.cz",
      lastSeenAt: iso(-8),
    },
    secondTeacher,
  ];
  const students: ClassMember[] = (
    [
      ["demo-student", "male"],
      ["student-4b", "female"],
      ["student-9f", "male"],
      ["student-e2", "female"],
      ["student-e7", "male"],
    ] as const
  ).map(([studentId, sex]) => ({
    studentId,
    studentName: fullName(sex),
  }));
  const studentName = (studentId: string) =>
    students.find((student) => student.studentId === studentId)?.studentName ??
    "Žák bez jména";
  const technologyClassName = `${faker.helpers.arrayElement([2, 3, 4])}.${faker.helpers.arrayElement(["A", "B"])} · Informační technologie`;
  const electricalClassName = `${faker.helpers.arrayElement([2, 3, 4])}.${faker.helpers.arrayElement(["E", "F"])} · Elektrotechnika`;
  const networkQuestions: Question[] = [
    {
      id: 101,
      quizUuid: networkQuizUuid,
      question: "Která vrstva modelu OSI směruje pakety mezi sítěmi?",
      type: "MULTIPLE_CHOICE",
      points: 2,
      timeInSeconds: 30,
      codeSnippet: null,
      imageRef: null,
      answers: [
        { answer: "Síťová", isCorrect: true },
        { answer: "Relační", isCorrect: false },
        { answer: "Prezentační", isCorrect: false },
        { answer: "Fyzická", isCorrect: false },
      ],
      pairs: [],
    },
    {
      id: 102,
      quizUuid: networkQuizUuid,
      question: "Přiřaď protokol k jeho běžnému portu.",
      type: "MATCHING",
      points: 3,
      timeInSeconds: 45,
      codeSnippet: null,
      imageRef: null,
      answers: [],
      pairs: [
        { left: "HTTPS", right: "443" },
        { left: "SSH", right: "22" },
        { left: "DNS", right: "53" },
      ],
    },
    {
      id: 103,
      quizUuid: networkQuizUuid,
      question: "Vysvětli jednou větou, k čemu slouží výchozí brána.",
      type: "FREE_TEXT",
      points: 3,
      timeInSeconds: 60,
      codeSnippet: null,
      imageRef: null,
      answers: [],
      pairs: [],
    },
  ];

  return {
    administrator: {
      id: "demo-administrator",
      name: `Bc. ${fullName("female")}`,
    },
    teacher,
    teachers,
    students,
    classes: [
      {
        uuid: technologyClassUuid,
        name: technologyClassName,
        studentCount: 3,
        members: students.slice(0, 3),
        createdAt: iso(-30_000),
        updatedAt: iso(-1_440),
        teacherIds: [teacher.id, secondTeacher.teacherId],
        active: true,
        version: 0,
        groups: [
          {
            uuid: technologyGroupAUuid,
            name: "Laboratorní skupina A",
            active: true,
            version: 0,
            members: students.slice(0, 2),
          },
          {
            uuid: technologyGroupBUuid,
            name: "Laboratorní skupina B",
            active: true,
            version: 0,
            members: students.slice(2, 3),
          },
        ],
      },
      {
        uuid: electricalClassUuid,
        name: electricalClassName,
        studentCount: 2,
        members: students.slice(3),
        createdAt: iso(-25_000),
        updatedAt: iso(-2_880),
        teacherIds: [secondTeacher.teacherId],
        active: true,
        version: 0,
        groups: [],
      },
    ],
    subjects: [
      {
        uuid: networkSubjectUuid,
        name: "Počítačové sítě",
        teacherIds: [teacher.id],
        createdAt: iso(-40_000),
        updatedAt: iso(-1_200),
      },
      {
        uuid: electricalSubjectUuid,
        name: "Elektrotechnika",
        teacherIds: [teacher.id, secondTeacher.teacherId],
        createdAt: iso(-38_000),
        updatedAt: iso(-950),
      },
    ],
    quizzes: [
      {
        uuid: networkQuizUuid,
        title: "Počítačové sítě · opakování",
        description: "Krátké ověření modelu OSI, portů a základní konfigurace sítě.",
        subject: "Počítačové sítě",
        subjectUuid: networkSubjectUuid,
        chapter: "Síťové protokoly",
        questions: networkQuestions,
        maxQuestionsPerSession: 3,
        shuffle: false,
        status: "RUNNING",
        createdAt: iso(-4_320),
        editedAt: iso(-120),
        validFrom: iso(-30),
        validTo: iso(1_440),
      },
      {
        uuid: "ddf29bd1-7d60-4b49-9ef2-bbf60d5e4062",
        title: "Ohmův zákon",
        description: "Příprava otázek k výpočtu napětí, proudu a odporu.",
        subject: "Elektrotechnika",
        subjectUuid: electricalSubjectUuid,
        chapter: "Základní veličiny",
        questions: [],
        maxQuestionsPerSession: 8,
        shuffle: true,
        status: "DRAFT",
        createdAt: iso(-1_440),
        editedAt: iso(-90),
        validFrom: null,
        validTo: null,
      },
      {
        uuid: "9478c533-c3cf-49d1-b52e-bd41e0e847d9",
        title: "Bezpečnost v dílně",
        description: "Uzavřené školení zásad práce v elektrotechnické dílně.",
        subject: "Odborný výcvik",
        subjectUuid: null,
        chapter: "BOZP",
        questions: networkQuestions.slice(0, 2).map((question, index) => ({
          ...question,
          id: 201 + index,
          quizUuid: "9478c533-c3cf-49d1-b52e-bd41e0e847d9",
        })),
        maxQuestionsPerSession: 2,
        shuffle: false,
        status: "ARCHIVED",
        createdAt: iso(-20_000),
        editedAt: iso(-15_000),
        validFrom: iso(-20_000),
        validTo: iso(-10_000),
      },
    ],
    sessions: [
      {
        sessionUuid: networkSessionUuid,
        teacherId: teacher.id,
        quizUuid: networkQuizUuid,
        title: "Počítačové sítě · opakování",
        description: "Tři otázky před dnešním cvičením.",
        subject: "Počítačové sítě",
        chapter: "Síťové protokoly",
        teacherName: teacher.name,
        questionCount: 3,
        startedAt: iso(-5),
        expiresAt: iso(45),
        openToAllStudents: false,
        assignedClasses: [
          {
            uuid: technologyClassUuid,
            name: technologyClassName,
          },
        ],
        eligibleStudentIds: ["demo-student", "student-4b", "student-9f"],
      },
      {
        sessionUuid: "5095290c-d931-4418-a85d-76b70cc03bd1",
        teacherId: secondTeacher.teacherId,
        quizUuid: "9478c533-c3cf-49d1-b52e-bd41e0e847d9",
        title: "Bezpečnost v dílně",
        description: "Opakování pravidel před odborným výcvikem.",
        subject: "Odborný výcvik",
        chapter: "BOZP",
        teacherName: secondTeacher.displayName,
        questionCount: 2,
        startedAt: iso(-8),
        expiresAt: iso(30),
        openToAllStudents: false,
        assignedClasses: [
          {
            uuid: electricalClassUuid,
            name: electricalClassName,
          },
        ],
        eligibleStudentIds: ["student-e2", "student-e7"],
      },
    ],
    results: [
      {
        id: 7101,
        teacherId: teacher.id,
        assignedClassUuids: [technologyClassUuid],
        sessionUuid: networkSessionUuid,
        quizUuid: networkQuizUuid,
        studentId: "student-4b",
        studentName: studentName("student-4b"),
        score: 2,
        maxScore: 8,
        playedAt: iso(-2),
        questionResults: [
          {
            id: 9100,
            questionIndex: 0,
            question: "Která vrstva modelu OSI směruje pakety mezi sítěmi?",
            type: "MULTIPLE_CHOICE",
            points: 2,
            awardedPoints: 2,
            status: "GRADED",
            text: null,
            selectedIndexes: [0],
            pairs: [],
          },
          {
            id: 9102,
            questionIndex: 1,
            question: "Přiřaď protokol k jeho běžnému portu.",
            type: "MATCHING",
            points: 3,
            awardedPoints: 0,
            status: "GRADED",
            text: null,
            selectedIndexes: [],
            pairs: [
              { leftIndex: 0, rightIndex: 0 },
              { leftIndex: 1, rightIndex: 2 },
              { leftIndex: 2, rightIndex: 1 },
            ],
          },
          {
            id: 9101,
            questionIndex: 2,
            question: "Vysvětli jednou větou, k čemu slouží výchozí brána.",
            type: "FREE_TEXT",
            points: 3,
            awardedPoints: null,
            status: "PENDING_REVIEW",
            text: "Posílá provoz do jiných sítí, když cíl není v místní síti.",
            selectedIndexes: [],
            pairs: [],
          },
        ],
      },
      {
        id: 7102,
        teacherId: teacher.id,
        assignedClassUuids: [technologyClassUuid],
        sessionUuid: networkSessionUuid,
        quizUuid: networkQuizUuid,
        studentId: "student-9f",
        studentName: studentName("student-9f"),
        score: 5,
        maxScore: 8,
        playedAt: iso(-1),
        questionResults: [
          {
            id: 9110,
            questionIndex: 0,
            question: "Která vrstva modelu OSI směruje pakety mezi sítěmi?",
            type: "MULTIPLE_CHOICE",
            points: 2,
            awardedPoints: 0,
            status: "GRADED",
            text: null,
            selectedIndexes: [1],
            pairs: [],
          },
          {
            id: 9111,
            questionIndex: 1,
            question: "Přiřaď protokol k jeho běžnému portu.",
            type: "MATCHING",
            points: 3,
            awardedPoints: 3,
            status: "GRADED",
            text: null,
            selectedIndexes: [],
            pairs: [
              { leftIndex: 0, rightIndex: 0 },
              { leftIndex: 1, rightIndex: 1 },
              { leftIndex: 2, rightIndex: 2 },
            ],
          },
          {
            id: 9112,
            questionIndex: 2,
            question: "Vysvětli jednou větou, k čemu slouží výchozí brána.",
            type: "FREE_TEXT",
            points: 3,
            awardedPoints: 2,
            status: "GRADED",
            text: "Předává provoz z místní sítě směrovači do ostatních sítí.",
            selectedIndexes: [],
            pairs: [],
          },
        ],
      },
      {
        id: 7001,
        teacherId: teacher.id,
        assignedClassUuids: [technologyClassUuid],
        sessionUuid: "e5c11872-24f2-4568-86dc-34786554e667",
        quizUuid: "9478c533-c3cf-49d1-b52e-bd41e0e847d9",
        studentId: "demo-student",
        studentName: studentName("demo-student"),
        score: 4,
        maxScore: 5,
        playedAt: iso(-2_880),
        questionResults: [
          {
            id: 8001,
            questionIndex: 0,
            question: "Která ochrana se používá při práci na elektrickém zařízení?",
            type: "MULTIPLE_CHOICE",
            points: 2,
            awardedPoints: 2,
            status: "GRADED",
            text: null,
            selectedIndexes: [0],
            pairs: [],
          },
          {
            id: 8002,
            questionIndex: 1,
            question: "Popiš bezpečný postup před zahájením práce.",
            type: "FREE_TEXT",
            points: 3,
            awardedPoints: 2,
            status: "GRADED",
            text: "Odpojit zařízení, ověřit beznapěťový stav a zajistit pracoviště.",
            selectedIndexes: [],
            pairs: [],
          },
        ],
      },
    ],
    pending: [
      {
        resultId: 9101,
        studentId: "student-4b",
        studentName: studentName("student-4b"),
        questionIndex: 2,
        question: "Vysvětli jednou větou, k čemu slouží výchozí brána.",
        text: "Posílá provoz do jiných sítí, když cíl není v místní síti.",
        points: 3,
        awardedPoints: null,
        status: "PENDING_REVIEW",
      },
    ],
    nextQuestionId: 300,
    nextResultId: 9200,
  };
}

function clone<T>(value: T): T {
  return structuredClone(value);
}

function normalizeDatabase(value: unknown): {
  database: DemoDatabase;
  migrated: boolean;
} {
  const defaults = seedDatabase();
  const stored =
    value && typeof value === "object"
      ? (value as Partial<DemoDatabase>)
      : {};
  let migrated = false;

  const administrator =
    stored.administrator &&
    typeof stored.administrator.id === "string" &&
    typeof stored.administrator.name === "string"
      ? stored.administrator
      : defaults.administrator;
  if (administrator !== stored.administrator) migrated = true;

  const teacher =
    stored.teacher &&
    typeof stored.teacher.id === "string" &&
    typeof stored.teacher.name === "string"
      ? stored.teacher
      : defaults.teacher;
  if (teacher !== stored.teacher) migrated = true;

  const teachers = Array.isArray(stored.teachers)
    ? stored.teachers
    : defaults.teachers;
  const subjects = Array.isArray(stored.subjects)
    ? stored.subjects
    : defaults.subjects;

  const students = Array.isArray(stored.students)
    ? stored.students
    : defaults.students;
  const rawClasses = Array.isArray(stored.classes)
    ? stored.classes
    : defaults.classes;
  const classes = rawClasses.map((schoolClass) => {
    if (Array.isArray(schoolClass.teacherIds) && schoolClass.teacherIds.length) {
      return schoolClass;
    }
    migrated = true;
    return { ...schoolClass, teacherIds: [teacher.id] };
  });
  const quizzes = Array.isArray(stored.quizzes) ? stored.quizzes : defaults.quizzes;
  const rawResults = Array.isArray(stored.results)
    ? stored.results
    : defaults.results;
  const results = rawResults.map((result) => {
    const assignedClassUuids = Array.isArray(result.assignedClassUuids)
      ? result.assignedClassUuids
      : [];
    const teacherId =
      typeof result.teacherId === "string" && result.teacherId
        ? result.teacherId
        : teacher.id;
    if (
      result.assignedClassUuids !== assignedClassUuids ||
      result.teacherId !== teacherId
    ) {
      migrated = true;
    }
    return { ...result, assignedClassUuids, teacherId };
  });
  const pending = Array.isArray(stored.pending) ? stored.pending : defaults.pending;
  const storedSessions = Array.isArray(stored.sessions)
    ? stored.sessions
    : defaults.sessions;

  if (
    students !== stored.students ||
    rawClasses !== stored.classes ||
    teachers !== stored.teachers ||
    subjects !== stored.subjects ||
    quizzes !== stored.quizzes ||
    rawResults !== stored.results ||
    pending !== stored.pending ||
    storedSessions !== stored.sessions
  ) {
    migrated = true;
  }

  const sessions = storedSessions.map((session) => {
    const assignedClasses = Array.isArray(session.assignedClasses)
      ? session.assignedClasses
      : [];
    const openToAllStudents =
      typeof session.openToAllStudents === "boolean"
        ? session.openToAllStudents
        : assignedClasses.length === 0;
    const eligibleStudentIds =
      session.eligibleStudentIds === null ||
      Array.isArray(session.eligibleStudentIds)
        ? session.eligibleStudentIds
        : openToAllStudents
          ? null
          : Array.from(
              new Set(
                classes
                  .filter((schoolClass) =>
                    assignedClasses.some(
                      (assignedClass) => assignedClass.uuid === schoolClass.uuid,
                    ),
                  )
                  .flatMap((schoolClass) =>
                    schoolClass.members.map((member) => member.studentId),
                  ),
              ),
            );
    const matchedTeacher = teachers.find(
      (candidate) =>
        candidate.teacherId === session.teacherId ||
        candidate.displayName === session.teacherName,
    );
    const teacherId = matchedTeacher?.teacherId ?? teacher.id;
    const teacherName =
      matchedTeacher?.displayName || session.teacherName || teacher.name;

    if (
      session.assignedClasses !== assignedClasses ||
      session.openToAllStudents !== openToAllStudents ||
      session.eligibleStudentIds !== eligibleStudentIds ||
      session.teacherId !== teacherId ||
      session.teacherName !== teacherName
    ) {
      migrated = true;
    }

    return {
      ...session,
      teacherId,
      teacherName,
      assignedClasses,
      openToAllStudents,
      eligibleStudentIds,
    };
  });

  const nextQuestionId = Number.isInteger(stored.nextQuestionId)
    ? stored.nextQuestionId as number
    : defaults.nextQuestionId;
  const nextResultId = Number.isInteger(stored.nextResultId)
    ? stored.nextResultId as number
    : defaults.nextResultId;
  if (
    nextQuestionId !== stored.nextQuestionId ||
    nextResultId !== stored.nextResultId
  ) {
    migrated = true;
  }

  return {
    database: {
      administrator,
      teacher,
      teachers,
      students,
      classes,
      subjects,
      quizzes,
      sessions,
      results,
      pending,
      nextQuestionId,
      nextResultId,
    },
    migrated,
  };
}

function readDatabase(): DemoDatabase {
  if (typeof window === "undefined") return seedDatabase();
  const stored = window.localStorage.getItem(DATABASE_KEY);
  if (!stored) {
    const seeded = seedDatabase();
    writeDatabase(seeded);
    return seeded;
  }

  try {
    const normalized = normalizeDatabase(JSON.parse(stored));
    const database = normalized.database;
    let shouldWrite = normalized.migrated;
    if (database.sessions.every((session) => new Date(session.expiresAt).getTime() <= Date.now())) {
      const freshSession = seedDatabase().sessions[0];
      database.sessions = [freshSession];
      shouldWrite = true;
    }
    if (shouldWrite) writeDatabase(database);
    return database;
  } catch {
    const seeded = seedDatabase();
    writeDatabase(seeded);
    return seeded;
  }
}

function writeDatabase(database: DemoDatabase) {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(DATABASE_KEY, JSON.stringify(database));
    window.dispatchEvent(new Event(DEMO_DATABASE_CHANGED_EVENT));
  }
}

function requireQuiz(database: DemoDatabase, uuid: string) {
  const quiz = database.quizzes.find((candidate) => candidate.uuid === uuid);
  if (!quiz) throw demoProblem(404, "quiz_not_found", "Quiz not found.");
  return quiz;
}

function currentRole(): DemoRole | null {
  if (typeof window === "undefined") return null;
  const role = window.sessionStorage.getItem(ROLE_KEY);
  return role === "ADMINISTRATOR" || role === "TEACHER" || role === "STUDENT"
    ? role
    : null;
}

export function demoLogin(role: DemoRole) {
  window.sessionStorage.setItem(ROLE_KEY, role);
}

export function demoLogout() {
  window.sessionStorage.removeItem(ROLE_KEY);
  window.sessionStorage.removeItem(ATTEMPT_KEY);
}

export function demoMe(): Me {
  const role = currentRole();
  if (!role) {
    throw demoProblem(401, "authentication_required", "Authentication is required.");
  }
  const database = readDatabase();
  if (role === "ADMINISTRATOR") {
    return {
      sub: database.administrator.id,
      name: database.administrator.name,
      roles: [role],
    };
  }
  if (role === "TEACHER") {
    return {
      sub: database.teacher.id,
      name: database.teacher.name,
      roles: [role],
    };
  }
  const student = database.students.find(
    (candidate) => candidate.studentId === "demo-student",
  );
  return {
    sub: student?.studentId ?? "demo-student",
    name: student?.studentName ?? "Žák bez jména",
    roles: [role],
  };
}

function requireDemoTeacher() {
  if (currentRole() !== "TEACHER") {
    throw demoProblem(
      403,
      "access_denied",
      "You are not allowed to perform this action.",
    );
  }
}

function requireDemoAdministrator() {
  if (currentRole() !== "ADMINISTRATOR") {
    throw demoProblem(
      403,
      "access_denied",
      "You are not allowed to perform this action.",
    );
  }
}

function classMembersFor(
  database: DemoDatabase,
  studentIds: string[],
) {
  const knownNames = new Map(
    database.students.map((student) => [student.studentId, student.studentName]),
  );

  return Array.from(new Set(studentIds)).map((studentId) => ({
    studentId,
    studentName: knownNames.get(studentId) ?? "Žák bez jména",
  }));
}

function replaceClassMembers(
  database: DemoDatabase,
  schoolClass: DemoClass,
  studentIds: string[],
) {
  schoolClass.members = classMembersFor(database, studentIds);
  schoolClass.studentCount = schoolClass.members.length;
  const memberIds = new Set(
    schoolClass.members.map((member) => member.studentId),
  );
  schoolClass.groups?.forEach((group) => {
    group.members = group.members.filter((member) =>
      memberIds.has(member.studentId),
    );
  });
}

export function demoListClasses() {
  requireDemoTeacher();
  const database = readDatabase();
  return clone(
    database.classes.filter(
      (schoolClass) => schoolClass.teacherIds.includes(database.teacher.id),
    ),
  );
}

export function demoGetClass(classUuid: string) {
  requireDemoTeacher();
  const database = readDatabase();
  const schoolClass = database.classes.find(
    (candidate) =>
      candidate.uuid === classUuid &&
      candidate.teacherIds.includes(database.teacher.id),
  );
  if (!schoolClass) {
    throw demoProblem(
      404,
      "class_not_found",
      "The class is not assigned to this teacher.",
    );
  }
  return clone(schoolClass);
}

function teacherClassForGroupManagement(
  database: DemoDatabase,
  classUuid: string,
) {
  const schoolClass = database.classes.find(
    (candidate) =>
      candidate.uuid === classUuid &&
      candidate.teacherIds.includes(database.teacher.id),
  );
  if (!schoolClass) {
    throw demoProblem(
      404,
      "class_not_found",
      "The class is not assigned to this teacher.",
    );
  }
  if (schoolClass.active === false) {
    throw demoProblem(409, "class_inactive", "This class is inactive.");
  }
  return schoolClass;
}

export function demoCreateGroup(classUuid: string, value: string) {
  requireDemoTeacher();
  const database = readDatabase();
  const schoolClass = teacherClassForGroupManagement(database, classUuid);
  const name = value.trim();
  if (!name) {
    throw demoProblem(400, "name_required", "A non-blank name is required.");
  }
  if (name.length > 100) {
    throw demoProblem(
      400,
      "validation_failed",
      "The group name must contain at most 100 characters.",
    );
  }
  if (
    schoolClass.groups?.some(
      (group) =>
        group.name.localeCompare(name, "cs", { sensitivity: "accent" }) === 0,
    )
  ) {
    throw demoProblem(
      409,
      "group_name_exists",
      "A group with that name already exists in this class.",
    );
  }

  schoolClass.groups = [
    ...(schoolClass.groups ?? []),
    {
      uuid: crypto.randomUUID(),
      name,
      active: true,
      version: 0,
      members: [],
    },
  ];
  writeDatabase(database);
  return clone(schoolClass);
}

export function demoSetGroupStudent(
  classUuid: string,
  groupUuid: string,
  studentId: string,
  assigned: boolean,
) {
  requireDemoTeacher();
  const database = readDatabase();
  const schoolClass = teacherClassForGroupManagement(database, classUuid);
  const group = schoolClass.groups?.find(
    (candidate) => candidate.uuid === groupUuid,
  );
  if (!group) {
    throw demoProblem(
      404,
      "group_not_found",
      "The group does not exist in this class.",
    );
  }

  if (assigned) {
    if (!group.active) {
      throw demoProblem(
        409,
        "group_inactive",
        "Students cannot be assigned to an inactive group.",
      );
    }
    const student = schoolClass.members.find(
      (candidate) => candidate.studentId === studentId,
    );
    if (!student) {
      throw demoProblem(
        404,
        "student_not_in_class",
        "The student does not belong to this class.",
      );
    }
    if (!group.members.some((member) => member.studentId === studentId)) {
      group.members.push(clone(student));
    }
  } else {
    group.members = group.members.filter((member) => member.studentId !== studentId);
  }

  writeDatabase(database);
  return clone(schoolClass);
}

export function demoCreateClass(input: ClassInput) {
  requireDemoTeacher();
  const name = input.name.trim();
  if (!name) {
    throw demoProblem(400, "name_required", "A non-blank name is required.");
  }

  const database = readDatabase();
  const members = classMembersFor(database, input.studentIds ?? []);
  const schoolClass: DemoClass = {
    uuid: crypto.randomUUID(),
    name,
    studentCount: members.length,
    members,
    createdAt: iso(),
    updatedAt: iso(),
    teacherIds: [database.teacher.id],
  };
  database.classes.unshift(schoolClass);
  writeDatabase(database);
  return clone(schoolClass);
}

export function demoUpdateClass(
  classUuid: string,
  input: Partial<ClassInput>,
) {
  requireDemoTeacher();
  const database = readDatabase();
  const schoolClass = database.classes.find(
    (candidate) =>
      candidate.uuid === classUuid &&
      candidate.teacherIds.includes(database.teacher.id),
  );
  if (!schoolClass) {
    throw demoProblem(
      404,
      "class_not_found",
      "The class is not assigned to this teacher.",
    );
  }

  if (input.name !== undefined) {
    const name = input.name.trim();
    if (!name) {
      throw demoProblem(400, "name_required", "A non-blank name is required.");
    }
    schoolClass.name = name;
  }
  if (input.studentIds !== undefined) {
    replaceClassMembers(database, schoolClass, input.studentIds);
  }
  schoolClass.updatedAt = iso();
  writeDatabase(database);
  return clone(schoolClass);
}

export function demoDeleteClass(classUuid: string) {
  requireDemoTeacher();
  const database = readDatabase();
  if (
    !database.classes.some(
      (candidate) =>
        candidate.uuid === classUuid &&
        candidate.teacherIds.includes(database.teacher.id),
    )
  ) {
    throw demoProblem(
      404,
      "class_not_found",
      "The class is not assigned to this teacher.",
    );
  }
  database.classes = database.classes.filter(
    (candidate) =>
      candidate.uuid !== classUuid ||
      !candidate.teacherIds.includes(database.teacher.id),
  );
  writeDatabase(database);
}

function requireKnownTeacher(database: DemoDatabase, teacherId: string) {
  const teacher = database.teachers.find(
    (candidate) => candidate.teacherId === teacherId,
  );
  if (!teacher) {
    throw demoProblem(
      404,
      "teacher_not_found",
      "No active verified teacher has that id.",
    );
  }
  return teacher;
}

function adminClassResponse(
  database: DemoDatabase,
  schoolClass: DemoClass,
): AdminClassResponse {
  const teachers = schoolClass.teacherIds.map((teacherId) =>
    requireKnownTeacher(database, teacherId),
  );
  return {
    uuid: schoolClass.uuid,
    name: schoolClass.name,
    teacherCount: teachers.length,
    teachers: teachers.map(
      ({ teacherId, displayName, preferredUsername }) => ({
        teacherId,
        displayName,
        preferredUsername,
      }),
    ),
    studentCount: schoolClass.studentCount,
    members: schoolClass.members,
    createdAt: schoolClass.createdAt,
    updatedAt: schoolClass.updatedAt,
    active: schoolClass.active,
    version: schoolClass.version,
    groups: schoolClass.groups,
  };
}

function adminSubjectResponse(
  database: DemoDatabase,
  subject: DemoSubject,
): AdminSubjectResponse {
  const teachers = subject.teacherIds
    .map((teacherId) =>
      database.teachers.find((teacher) => teacher.teacherId === teacherId),
    )
    .filter((teacher): teacher is AdminTeacherResponse => Boolean(teacher));
  return {
    uuid: subject.uuid,
    name: subject.name,
    teacherCount: teachers.length,
    teachers,
    createdAt: subject.createdAt,
    updatedAt: subject.updatedAt,
  };
}

export function demoAdminTeachers() {
  requireDemoAdministrator();
  return clone(readDatabase().teachers);
}

export function demoAdminStudents() {
  requireDemoAdministrator();
  return clone(readDatabase().students);
}

export function demoAdminClasses() {
  requireDemoAdministrator();
  const database = readDatabase();
  return clone(
    database.classes.map((schoolClass) =>
      adminClassResponse(database, schoolClass),
    ),
  );
}

export function demoAdminGetClass(classUuid: string) {
  requireDemoAdministrator();
  const database = readDatabase();
  const schoolClass = database.classes.find(
    (candidate) => candidate.uuid === classUuid,
  );
  if (!schoolClass) {
    throw demoProblem(404, "class_not_found", "The class does not exist.");
  }
  return clone(adminClassResponse(database, schoolClass));
}

export function demoAdminCreateClass(input: AdminClassInput) {
  requireDemoAdministrator();
  const database = readDatabase();
  const name = input.name.trim();
  if (!name) {
    throw demoProblem(400, "name_required", "A non-blank name is required.");
  }
  const teacherIds = Array.from(new Set(input.teacherIds));
  if (teacherIds.length === 0) {
    throw demoProblem(
      400,
      "class_teacher_required",
      "At least one teacher must be assigned to the class.",
    );
  }
  teacherIds.forEach((teacherId) => requireKnownTeacher(database, teacherId));
  const members = classMembersFor(database, input.studentIds ?? []);
  const schoolClass: DemoClass = {
    uuid: crypto.randomUUID(),
    name,
    teacherIds,
    studentCount: members.length,
    members,
    createdAt: iso(),
    updatedAt: iso(),
  };
  database.classes.unshift(schoolClass);
  writeDatabase(database);
  return clone(adminClassResponse(database, schoolClass));
}

export function demoAdminUpdateClass(
  classUuid: string,
  input: Partial<AdminClassInput>,
) {
  requireDemoAdministrator();
  const database = readDatabase();
  const schoolClass = database.classes.find(
    (candidate) => candidate.uuid === classUuid,
  );
  if (!schoolClass) {
    throw demoProblem(404, "class_not_found", "The class does not exist.");
  }
  if (input.name !== undefined) {
    const name = input.name.trim();
    if (!name) {
      throw demoProblem(400, "name_required", "A non-blank name is required.");
    }
    schoolClass.name = name;
  }
  if (input.teacherIds !== undefined) {
    const teacherIds = Array.from(new Set(input.teacherIds));
    if (teacherIds.length === 0) {
      throw demoProblem(
        400,
        "class_teacher_required",
        "At least one teacher must be assigned to the class.",
      );
    }
    teacherIds.forEach((teacherId) => requireKnownTeacher(database, teacherId));
    schoolClass.teacherIds = teacherIds;
  }
  if (input.studentIds !== undefined) {
    replaceClassMembers(database, schoolClass, input.studentIds);
  }
  schoolClass.updatedAt = iso();
  writeDatabase(database);
  return clone(adminClassResponse(database, schoolClass));
}

export function demoAdminDeleteClass(classUuid: string) {
  requireDemoAdministrator();
  const database = readDatabase();
  if (!database.classes.some((candidate) => candidate.uuid === classUuid)) {
    throw demoProblem(404, "class_not_found", "The class does not exist.");
  }
  database.classes = database.classes.filter(
    (candidate) => candidate.uuid !== classUuid,
  );
  writeDatabase(database);
}

export function demoAssignedSubjects() {
  requireDemoTeacher();
  const database = readDatabase();
  return clone(
    database.subjects
      .filter((subject) => subject.teacherIds.includes(database.teacher.id))
      .map(({ uuid, name }) => ({ uuid, name })),
  );
}

export function demoAdminSubjects() {
  requireDemoAdministrator();
  const database = readDatabase();
  return clone(
    database.subjects.map((subject) =>
      adminSubjectResponse(database, subject),
    ),
  );
}

export function demoAdminGetSubject(subjectUuid: string) {
  requireDemoAdministrator();
  const database = readDatabase();
  const subject = database.subjects.find(
    (candidate) => candidate.uuid === subjectUuid,
  );
  if (!subject) {
    throw demoProblem(404, "subject_not_found", "The subject does not exist.");
  }
  return clone(adminSubjectResponse(database, subject));
}

export function demoAdminCreateSubject(input: AdminSubjectInput) {
  requireDemoAdministrator();
  const database = readDatabase();
  const name = input.name.trim();
  if (!name) {
    throw demoProblem(400, "name_required", "A non-blank name is required.");
  }
  if (
    database.subjects.some(
      (subject) =>
        subject.name.toLocaleLowerCase("cs") === name.toLocaleLowerCase("cs"),
    )
  ) {
    throw demoProblem(
      409,
      "subject_name_exists",
      "A subject with that name already exists.",
    );
  }
  const teacherIds = Array.from(new Set(input.teacherIds ?? []));
  teacherIds.forEach((teacherId) => requireKnownTeacher(database, teacherId));
  const subject: DemoSubject = {
    uuid: crypto.randomUUID(),
    name,
    teacherIds,
    createdAt: iso(),
    updatedAt: iso(),
  };
  database.subjects.unshift(subject);
  writeDatabase(database);
  return clone(adminSubjectResponse(database, subject));
}

export function demoAdminUpdateSubject(
  subjectUuid: string,
  input: Partial<AdminSubjectInput>,
) {
  requireDemoAdministrator();
  const database = readDatabase();
  const subject = database.subjects.find(
    (candidate) => candidate.uuid === subjectUuid,
  );
  if (!subject) {
    throw demoProblem(404, "subject_not_found", "The subject does not exist.");
  }
  if (input.name !== undefined) {
    const name = input.name.trim();
    if (!name) {
      throw demoProblem(400, "name_required", "A non-blank name is required.");
    }
    subject.name = name;
    database.quizzes.forEach((quiz) => {
      if (quiz.subjectUuid === subjectUuid) quiz.subject = name;
    });
  }
  if (input.teacherIds !== undefined) {
    const teacherIds = Array.from(new Set(input.teacherIds));
    teacherIds.forEach((teacherId) => requireKnownTeacher(database, teacherId));
    subject.teacherIds = teacherIds;
  }
  subject.updatedAt = iso();
  writeDatabase(database);
  return clone(adminSubjectResponse(database, subject));
}

export function demoAdminDeleteSubject(subjectUuid: string) {
  requireDemoAdministrator();
  const database = readDatabase();
  if (!database.subjects.some((candidate) => candidate.uuid === subjectUuid)) {
    throw demoProblem(404, "subject_not_found", "The subject does not exist.");
  }
  database.subjects = database.subjects.filter(
    (candidate) => candidate.uuid !== subjectUuid,
  );
  database.quizzes.forEach((quiz) => {
    if (quiz.subjectUuid === subjectUuid) quiz.subjectUuid = null;
  });
  writeDatabase(database);
}

function resolveAssignedSubject(
  database: DemoDatabase,
  input: { subject?: string; subjectUuid?: string | null },
) {
  const available = database.subjects.filter((subject) =>
    subject.teacherIds.includes(database.teacher.id),
  );
  if (input.subjectUuid) {
    const subject = available.find(
      (candidate) => candidate.uuid === input.subjectUuid,
    );
    if (!subject) {
      throw demoProblem(
        404,
        "subject_not_assigned",
        "The subject is not assigned to this teacher.",
      );
    }
    return subject;
  }

  const name = input.subject?.trim();
  if (!name) return null;
  const matches = available.filter(
    (subject) =>
      subject.name.toLocaleLowerCase("cs") === name.toLocaleLowerCase("cs"),
  );
  if (matches.length !== 1) {
    throw demoProblem(
      400,
      "quiz_yaml_invalid",
      "The YAML subject must match exactly one subject assigned to this teacher.",
    );
  }
  return matches[0];
}

export function demoListQuizzes(page: number, size: number): Page<Quiz> {
  const quizzes = readDatabase().quizzes;
  const start = page * size;
  const content = quizzes.slice(start, start + size);
  const totalPages = Math.max(1, Math.ceil(quizzes.length / size));
  return {
    content: clone(content),
    totalElements: quizzes.length,
    totalPages,
    number: page,
    size,
    first: page === 0,
    last: page >= totalPages - 1,
  };
}

export function demoAvailableQuizzes() {
  const now = Date.now();
  return clone(
    readDatabase().quizzes.filter((quiz) => {
      const statusAvailable = quiz.status === "RUNNING" || quiz.status === null;
      const afterStart = !quiz.validFrom || new Date(quiz.validFrom).getTime() <= now;
      const beforeEnd = !quiz.validTo || new Date(quiz.validTo).getTime() >= now;
      return statusAvailable && afterStart && beforeEnd;
    }),
  );
}

export function demoGetQuiz(uuid: string) {
  return clone(requireQuiz(readDatabase(), uuid));
}

export function demoCreateQuiz(input: QuizInput) {
  const database = readDatabase();
  const uuid = crypto.randomUUID();
  const subject = resolveAssignedSubject(database, input);
  database.quizzes.unshift({
    uuid,
    title: input.title,
    description: input.description || null,
    subject: subject?.name ?? null,
    subjectUuid: subject?.uuid ?? null,
    chapter: input.chapter || null,
    questions: [],
    maxQuestionsPerSession: input.maxQuestionsPerSession,
    shuffle: input.shuffle ?? false,
    status: input.status ?? "DRAFT",
    createdAt: iso(),
    editedAt: iso(),
    validFrom: input.validFrom || null,
    validTo: input.validTo || null,
  });
  writeDatabase(database);
  return { quizUuid: uuid };
}

export function demoPatchQuiz(uuid: string, input: Partial<QuizInput>) {
  const database = readDatabase();
  const quiz = requireQuiz(database, uuid);
  const subjectChanged =
    input.subject !== undefined || input.subjectUuid !== undefined;
  const subject = subjectChanged ? resolveAssignedSubject(database, input) : null;
  Object.assign(quiz, {
    ...input,
    description: input.description ?? quiz.description,
    subject: subjectChanged ? subject?.name ?? null : quiz.subject,
    subjectUuid: subjectChanged ? subject?.uuid ?? null : quiz.subjectUuid,
    chapter: input.chapter ?? quiz.chapter,
    validFrom: input.validFrom ?? quiz.validFrom,
    validTo: input.validTo ?? quiz.validTo,
    editedAt: iso(),
  });
  writeDatabase(database);
}

export function demoDeleteQuiz(uuid: string) {
  const database = readDatabase();
  database.quizzes = database.quizzes.filter((quiz) => quiz.uuid !== uuid);
  database.sessions = database.sessions.filter((session) => session.quizUuid !== uuid);
  writeDatabase(database);
}

export async function demoImportQuiz(file: File) {
  const text = await file.text();
  let parsed: unknown;
  try {
    parsed = loadYaml(text);
  } catch {
    throw demoProblem(400, "quiz_yaml_invalid", "The YAML could not be parsed.");
  }
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    throw demoProblem(
      400,
      "quiz_yaml_invalid",
      "The YAML root must be a quiz object.",
    );
  }

  const input = parsed as Record<string, unknown>;
  const title = typeof input.title === "string" ? input.title.trim() : "";
  const rawQuestions = Array.isArray(input.questions) ? input.questions : [];
  if (!title || rawQuestions.length === 0) {
    throw demoProblem(
      400,
      "quiz_yaml_invalid",
      "The YAML must contain a title and at least one question.",
    );
  }

  const database = readDatabase();
  const subject = resolveAssignedSubject(database, {
    subject: typeof input.subject === "string" ? input.subject : undefined,
    subjectUuid:
      typeof input.subjectUuid === "string" ? input.subjectUuid : undefined,
  });
  const uuid = crypto.randomUUID();
  const questions = rawQuestions.map((rawQuestion) => {
    if (!rawQuestion || typeof rawQuestion !== "object" || Array.isArray(rawQuestion)) {
      throw demoProblem(
        400,
        "quiz_yaml_invalid",
        "Every YAML question must be an object.",
      );
    }
    const value = rawQuestion as Record<string, unknown>;
    const question = typeof value.question === "string" ? value.question.trim() : "";
    const type =
      value.type === "MATCHING" || value.type === "FREE_TEXT"
        ? value.type
        : "MULTIPLE_CHOICE";
    const points = Number.isInteger(value.points) ? Number(value.points) : 1;
    const timeInSeconds = Number.isInteger(value.timeInSeconds)
      ? Number(value.timeInSeconds)
      : 30;
    if (!question || points < 1 || timeInSeconds < 1) {
      throw demoProblem(
        400,
        "quiz_yaml_invalid",
        "A question has invalid text, points, or time limit.",
      );
    }

    const options = Array.isArray(value.options)
      ? value.options.map((option) => String(option))
      : [];
    const correctIndexes = new Set(
      Array.isArray(value.correctIndexes)
        ? value.correctIndexes.map((index) => Number(index))
        : [],
    );
    const rawPairs = Array.isArray(value.pairs) ? value.pairs : [];
    const pairs = rawPairs.map((pair) => {
      if (!pair || typeof pair !== "object" || Array.isArray(pair)) {
        throw demoProblem(
          400,
          "quiz_yaml_invalid",
          "A matching pair has an invalid format.",
        );
      }
      const pairValue = pair as Record<string, unknown>;
      return {
        left: String(pairValue.left ?? ""),
        right: String(pairValue.right ?? ""),
      };
    });

    if (
      (type === "MULTIPLE_CHOICE" &&
        (options.length < 2 || correctIndexes.size === 0)) ||
      (type === "MATCHING" && pairs.length === 0)
    ) {
      throw demoProblem(
        400,
        "quiz_yaml_invalid",
        "The question content does not match its type.",
      );
    }

    return {
      id: database.nextQuestionId++,
      quizUuid: uuid,
      question,
      type,
      points,
      timeInSeconds,
      codeSnippet:
        typeof value.codeSnippet === "string" ? value.codeSnippet : null,
      imageRef: typeof value.imageRef === "string" ? value.imageRef : null,
      answers:
        type === "MULTIPLE_CHOICE"
          ? options.map((answer, index) => ({
              answer,
              isCorrect: correctIndexes.has(index),
            }))
          : [],
      pairs: type === "MATCHING" ? pairs : [],
    } satisfies Question;
  });

  const requestedMaximum = Number.isInteger(input.maxQuestionsPerSession)
    ? Number(input.maxQuestionsPerSession)
    : questions.length;
  database.quizzes.unshift({
    uuid,
    title,
    description:
      typeof input.description === "string" ? input.description : null,
    subject: subject?.name ?? null,
    subjectUuid: subject?.uuid ?? null,
    chapter: typeof input.chapter === "string" ? input.chapter : null,
    questions,
    maxQuestionsPerSession: Math.max(1, requestedMaximum),
    shuffle: input.shuffle === true,
    status: "DRAFT",
    createdAt: iso(),
    editedAt: iso(),
    validFrom: null,
    validTo: null,
  });
  writeDatabase(database);
  return { quizUuid: uuid };
}

export function demoListQuestions(quizUuid: string) {
  return clone(requireQuiz(readDatabase(), quizUuid).questions);
}

export function demoCreateQuestion(quizUuid: string, input: QuestionInput) {
  const database = readDatabase();
  const quiz = requireQuiz(database, quizUuid);
  quiz.questions.push({
    id: database.nextQuestionId++,
    quizUuid,
    ...input,
    codeSnippet: input.codeSnippet || null,
    imageRef: input.imageRef || null,
    answers: input.answers ?? [],
    pairs: input.pairs ?? [],
  });
  quiz.editedAt = iso();
  writeDatabase(database);
}

export function demoPatchQuestion(
  quizUuid: string,
  questionId: number,
  input: Partial<QuestionInput>,
) {
  const database = readDatabase();
  const quiz = requireQuiz(database, quizUuid);
  const question = quiz.questions.find((candidate) => candidate.id === questionId);
  if (!question) {
    throw demoProblem(404, "question_not_found", "The question was not found.");
  }
  Object.assign(question, input);
  quiz.editedAt = iso();
  writeDatabase(database);
}

export function demoDeleteQuestion(quizUuid: string, questionId: number) {
  const database = readDatabase();
  const quiz = requireQuiz(database, quizUuid);
  quiz.questions = quiz.questions.filter((question) => question.id !== questionId);
  quiz.editedAt = iso();
  writeDatabase(database);
}

function studentCanOpenSession(
  session: DemoSession,
  studentId: string,
) {
  if (session.openToAllStudents) return true;
  return session.eligibleStudentIds?.includes(studentId) ?? false;
}

function publicDemoSession(session: DemoSession): ActiveSession {
  return {
    sessionUuid: session.sessionUuid,
    quizUuid: session.quizUuid,
    title: session.title,
    description: session.description,
    subject: session.subject,
    chapter: session.chapter,
    teacherName: session.teacherName,
    questionCount: session.questionCount,
    startedAt: session.startedAt,
    expiresAt: session.expiresAt,
    openToAllStudents: session.openToAllStudents,
    assignedClasses: session.assignedClasses,
  };
}

export function demoActiveSessions() {
  const database = readDatabase();
  const active = database.sessions.filter(
    (session) => new Date(session.expiresAt).getTime() > Date.now(),
  );
  if (currentRole() === "TEACHER") {
    return clone(
      active
        .filter((session) => session.teacherId === database.teacher.id)
        .map(publicDemoSession),
    );
  }
  if (currentRole() !== "STUDENT") return [];
  return clone(
    active.filter((session) =>
      studentCanOpenSession(session, demoMe().sub),
    ).map(publicDemoSession),
  );
}

function sessionQuiz(sessionUuid: string) {
  const database = readDatabase();
  const session = database.sessions.find((candidate) => candidate.sessionUuid === sessionUuid);
  if (!session) {
    throw demoProblem(404, "session.not_found", "The session was not found.");
  }
  if (new Date(session.expiresAt).getTime() <= Date.now()) {
    throw demoProblem(409, "session.closed", "The session is no longer active.");
  }
  if (
    currentRole() === "STUDENT" &&
    !studentCanOpenSession(session, demoMe().sub)
  ) {
    throw demoProblem(404, "session.not_found", "The session was not found.");
  }
  return { database, session, quiz: requireQuiz(database, session.quizUuid) };
}

function maskQuestion(question: Question) {
  return {
    ...clone(question),
    answers: question.answers.map((answer) => ({ ...answer, isCorrect: null })),
  };
}

function readAttempt() {
  if (typeof window === "undefined") return null;
  const stored = window.sessionStorage.getItem(ATTEMPT_KEY);
  return stored ? (JSON.parse(stored) as DemoAttempt) : null;
}

function writeAttempt(attempt: DemoAttempt) {
  window.sessionStorage.setItem(ATTEMPT_KEY, JSON.stringify(attempt));
}

export function demoJoinSession(sessionUuid: string) {
  const { quiz } = sessionQuiz(sessionUuid);
  if (!quiz.questions.length) {
    throw demoProblem(409, "quiz.empty", "The quiz does not contain any questions.");
  }
  writeAttempt({ sessionUuid, questionIndex: 0, submissions: [] });
  return maskQuestion(quiz.questions[0]);
}

export function demoNextQuestion(sessionUuid: string, submission: QuestionSubmission) {
  const { quiz } = sessionQuiz(sessionUuid);
  const attempt = readAttempt();
  if (!attempt || attempt.sessionUuid !== sessionUuid) {
    throw demoProblem(
      409,
      "attempt.not_active",
      "The attempt is not active in this browser.",
    );
  }
  attempt.submissions[attempt.questionIndex] = submission;
  attempt.questionIndex += 1;
  writeAttempt(attempt);
  if (attempt.questionIndex >= quiz.questions.length) {
    throw demoProblem(
      409,
      "attempt_ready_to_submit",
      "The attempt is ready to submit.",
    );
  }
  return maskQuestion(quiz.questions[attempt.questionIndex]);
}

function pointsFor(question: Question, submission: QuestionSubmission | undefined) {
  if (!submission || submission.type !== question.type) return 0;
  if (question.type === "MULTIPLE_CHOICE" && submission.type === "MULTIPLE_CHOICE") {
    const correct = question.answers
      .map((answer, index) => (answer.isCorrect ? index : -1))
      .filter((index) => index >= 0);
    const selected = [...submission.selectedIndexes].sort((a, b) => a - b);
    return JSON.stringify(correct) === JSON.stringify(selected) ? question.points : 0;
  }
  if (question.type === "MATCHING" && submission.type === "MATCHING") {
    const correct = submission.pairs.every((pair) => pair.leftIndex === pair.rightIndex);
    return correct && submission.pairs.length === question.pairs.length ? question.points : 0;
  }
  return 0;
}

export function demoFinishSession(sessionUuid: string): FinishResult {
  const { database, session, quiz } = sessionQuiz(sessionUuid);
  const attempt = readAttempt();
  if (!attempt || attempt.sessionUuid !== sessionUuid) {
    throw demoProblem(
      409,
      "attempt.not_active",
      "The attempt is not active in this browser.",
    );
  }

  const awarded = quiz.questions.map((question, index) =>
    pointsFor(question, attempt.submissions[index]),
  );
  const score = awarded.reduce((sum, points) => sum + points, 0);
  const maxScore = quiz.questions.reduce((sum, question) => sum + question.points, 0);
  const resultId = database.nextResultId++;

  database.results.unshift({
    id: resultId,
    teacherId: session.teacherId,
    assignedClassUuids: session.assignedClasses.map(
      (schoolClass) => schoolClass.uuid,
    ),
    sessionUuid,
    quizUuid: session.quizUuid,
    studentId: demoMe().sub,
    studentName: demoMe().name,
    score,
    maxScore,
    playedAt: iso(),
    questionResults: quiz.questions.map((question, index) => {
      const submission = attempt.submissions[index];
      const freeText = submission?.type === "FREE_TEXT" ? submission.text : null;
      return {
        id: resultId * 10 + index,
        questionIndex: index,
        question: question.question,
        type: question.type,
        points: question.points,
        awardedPoints: question.type === "FREE_TEXT" ? null : awarded[index],
        status: question.type === "FREE_TEXT" ? "PENDING_REVIEW" : "GRADED",
        text: freeText,
        selectedIndexes:
          submission?.type === "MULTIPLE_CHOICE" ? submission.selectedIndexes : [],
        pairs: submission?.type === "MATCHING" ? submission.pairs : [],
      };
    }),
  });

  quiz.questions.forEach((question, index) => {
    const submission = attempt.submissions[index];
    if (question.type === "FREE_TEXT" && submission?.type === "FREE_TEXT") {
      database.pending.push({
        resultId: resultId * 10 + index,
        studentId: demoMe().sub,
        studentName: demoMe().name,
        questionIndex: index,
        question: question.question,
        text: submission.text,
        points: question.points,
        awardedPoints: null,
        status: "PENDING_REVIEW",
      });
    }
  });

  writeDatabase(database);
  window.sessionStorage.removeItem(ATTEMPT_KEY);
  return {
    score,
    maxScore,
    pendingReviewCount: quiz.questions.filter(
      (question) => question.type === "FREE_TEXT",
    ).length,
    questions: clone(quiz.questions),
  };
}

export function demoCreateSession(
  quizUuid: string,
  expiresInMinutes: number,
  classIds: string[] = [],
  groupIds: string[] = [],
) {
  requireDemoTeacher();
  const database = readDatabase();
  const quiz = requireQuiz(database, quizUuid);
  if (!quiz.questions.length) {
    throw demoProblem(409, "quiz.empty", "The quiz does not contain any questions.");
  }
  const uniqueClassIds = Array.from(new Set(classIds));
  const uniqueGroupIds = Array.from(new Set(groupIds));
  if (uniqueClassIds.length === 0 && uniqueGroupIds.length === 0) {
    throw demoProblem(
      400,
      "audience_required",
      "At least one class or group must be selected.",
    );
  }
  const assignedClasses = uniqueClassIds.map((classUuid) => {
    const schoolClass = database.classes.find(
      (candidate) =>
        candidate.uuid === classUuid &&
        candidate.active !== false &&
        candidate.teacherIds.includes(database.teacher.id),
    );
    if (!schoolClass) {
      throw demoProblem(
        404,
        "class_not_found",
        "The class is not assigned to this teacher.",
      );
    }
    return { uuid: schoolClass.uuid, name: schoolClass.name };
  });
  const assignedGroups = uniqueGroupIds.map((groupUuid) => {
    const schoolClass = database.classes.find((candidate) =>
      candidate.active !== false &&
      candidate.teacherIds.includes(database.teacher.id) &&
      candidate.groups?.some((group) => group.uuid === groupUuid && group.active),
    );
    const group = schoolClass?.groups?.find((candidate) => candidate.uuid === groupUuid);
    if (!schoolClass || !group) {
      throw demoProblem(
        404,
        "group_not_found",
        "One or more selected groups do not exist.",
      );
    }
    if (uniqueClassIds.includes(schoolClass.uuid)) {
      throw demoProblem(
        400,
        "redundant_audience_target",
        "A whole class and one of its groups cannot both be selected.",
      );
    }
    return { schoolClass, group };
  });
  const eligibleStudentIds = Array.from(
    new Set([
      ...database.classes
        .filter((schoolClass) => uniqueClassIds.includes(schoolClass.uuid))
        .flatMap((schoolClass) =>
          schoolClass.members.map((member) => member.studentId),
        ),
      ...assignedGroups.flatMap(({ group }) =>
        group.members.map((member) => member.studentId),
      ),
    ]),
  );
  if (eligibleStudentIds.length === 0) {
    throw demoProblem(
      400,
      "audience_empty",
      "The selected audience has no active students.",
    );
  }
  const sessionUuid = crypto.randomUUID();
  database.sessions.unshift({
    sessionUuid,
    teacherId: database.teacher.id,
    quizUuid,
    title: quiz.title,
    description: quiz.description,
    subject: quiz.subject,
    chapter: quiz.chapter,
    teacherName: demoMe().name,
    questionCount: Math.min(quiz.questions.length, quiz.maxQuestionsPerSession),
    startedAt: iso(),
    expiresAt: iso(expiresInMinutes),
    openToAllStudents: false,
    assignedClasses: [
      ...assignedClasses,
      ...assignedGroups.map(({ schoolClass, group }) => ({
        uuid: group.uuid,
        name: `${schoolClass.name} · ${group.name}`,
      })),
    ],
    eligibleStudentIds,
  });
  writeDatabase(database);
  return sessionUuid;
}

export function demoMyResults() {
  const me = demoMe();
  return clone(readDatabase().results.filter((result) => result.studentId === me.sub));
}

export function demoClassLeaderboards(): ClassLeaderboard[] {
  if (currentRole() !== "STUDENT") {
    throw demoProblem(
      403,
      "access_denied",
      "You are not allowed to perform this action.",
    );
  }
  const database = readDatabase();
  const currentStudentId = demoMe().sub;
  const ownClasses = database.classes.filter((schoolClass) =>
    schoolClass.members.some(
      (member) => member.studentId === currentStudentId,
    ),
  );

  return clone(
    ownClasses
      .map((schoolClass) => {
        const ranked = schoolClass.members
          .map((member) => {
            const attempts = database.results.filter(
              (result) =>
                result.studentId === member.studentId &&
                result.assignedClassUuids.includes(schoolClass.uuid),
            );
            const totalScore = attempts.reduce(
              (sum, attempt) => sum + attempt.score,
              0,
            );
            const totalMaxScore = attempts.reduce(
              (sum, attempt) => sum + attempt.maxScore,
              0,
            );
            return {
              studentName: member.studentName || "Žák bez jména",
              totalScore,
              totalMaxScore,
              percentage: clampedPercentage(totalScore, totalMaxScore),
              attemptCount: attempts.length,
              rank: 0,
              currentStudent: member.studentId === currentStudentId,
            };
          })
          .sort(
            (left, right) =>
              Number(right.attemptCount > 0) - Number(left.attemptCount > 0) ||
              right.percentage - left.percentage ||
              right.totalScore - left.totalScore ||
              left.studentName.localeCompare(right.studentName, "cs"),
          );

        ranked.forEach((member, index) => {
          const previous = ranked[index - 1];
          member.rank =
            previous &&
            previous.percentage === member.percentage &&
            previous.totalScore === member.totalScore &&
            (previous.attemptCount > 0) === (member.attemptCount > 0)
              ? previous.rank
              : index + 1;
        });

        return {
          classUuid: schoolClass.uuid,
          className: schoolClass.name,
          members: ranked,
        };
      })
      .sort((left, right) => left.className.localeCompare(right.className, "cs")),
  );
}

export function demoSessionResults(sessionUuid: string) {
  requireDemoTeacher();
  const database = readDatabase();
  return clone(
    database.results.filter(
      (result) =>
        result.teacherId === database.teacher.id &&
        result.sessionUuid === sessionUuid,
    ),
  );
}

function clampedPercentage(score: number, maxScore: number) {
  if (maxScore <= 0) return 0;
  return Math.max(0, Math.min(100, Math.round((score * 100) / maxScore)));
}

function teacherAttemptSummary(
  database: DemoDatabase,
  result: QuizResult,
): TeacherAttemptSummary {
  const quiz = database.quizzes.find(
    (candidate) => candidate.uuid === result.quizUuid,
  );
  const rosterName = database.students.find(
    (student) => student.studentId === result.studentId,
  )?.studentName;
  return {
    resultId: result.id,
    sessionUuid: result.sessionUuid,
    quizUuid: result.quizUuid,
    quizTitle: quiz?.title ?? "Smazaný kvíz",
    studentId: result.studentId,
    studentName: result.studentName ?? rosterName ?? null,
    score: result.score,
    maxScore: result.maxScore,
    percentage: clampedPercentage(result.score, result.maxScore),
    playedAt: result.playedAt,
    questionCount: result.questionResults.length,
    pendingReviewCount: result.questionResults.filter(
      (question) => question.status === "PENDING_REVIEW",
    ).length,
  };
}

export function demoTeacherSessionHistory(limit = 100): TeacherAttemptSummary[] {
  requireDemoTeacher();
  const database = readDatabase();
  return clone(
    [...database.results]
      .filter((result) => result.teacherId === database.teacher.id)
      .sort(
        (left, right) =>
          new Date(right.playedAt).getTime() - new Date(left.playedAt).getTime() ||
          Number(right.id) - Number(left.id),
      )
      .slice(0, Math.max(1, Math.min(limit, 200)))
      .map((result) => teacherAttemptSummary(database, result)),
  );
}

export function demoTeacherStudents(
  limit = 100,
  attemptLimit = 20,
): TeacherStudentSummary[] {
  requireDemoTeacher();
  const database = readDatabase();
  const grouped = new Map<string, QuizResult[]>();

  for (const result of database.results) {
    if (result.teacherId !== database.teacher.id) continue;
    const attempts = grouped.get(result.studentId) ?? [];
    attempts.push(result);
    grouped.set(result.studentId, attempts);
  }

  const students = [...grouped.entries()]
    .map(([studentId, unsortedAttempts]) => {
      const attempts = [...unsortedAttempts].sort(
        (left, right) =>
          new Date(right.playedAt).getTime() - new Date(left.playedAt).getTime() ||
          Number(right.id) - Number(left.id),
      );
      const totalScore = attempts.reduce(
        (sum, attempt) => sum + attempt.score,
        0,
      );
      const totalMaxScore = attempts.reduce(
        (sum, attempt) => sum + attempt.maxScore,
        0,
      );
      const rosterName = database.students.find(
        (student) => student.studentId === studentId,
      )?.studentName;

      return {
        studentId,
        studentName: attempts[0]?.studentName ?? rosterName ?? null,
        attemptCount: attempts.length,
        totalScore,
        totalMaxScore,
        percentage: clampedPercentage(totalScore, totalMaxScore),
        lastPlayedAt: attempts[0]?.playedAt ?? iso(),
        attempts: attempts
          .slice(0, Math.max(1, Math.min(attemptLimit, 20)))
          .map((attempt) => teacherAttemptSummary(database, attempt)),
      } satisfies TeacherStudentSummary;
    })
    .sort(
      (left, right) =>
        new Date(right.lastPlayedAt).getTime() -
        new Date(left.lastPlayedAt).getTime(),
    )
    .slice(0, Math.max(1, Math.min(limit, 100)));

  return clone(students);
}

export function demoPendingAnswers(sessionUuid: string) {
  requireDemoTeacher();
  const database = readDatabase();
  const questionResultIds = new Set(
    database.results
      .filter(
        (result) =>
          result.teacherId === database.teacher.id &&
          result.sessionUuid === sessionUuid,
      )
      .flatMap((result) => result.questionResults.map((question) => question.id)),
  );
  return clone(
    database.pending.filter((answer) => questionResultIds.has(answer.resultId)),
  );
}

export function demoGradeAnswer(
  sessionUuid: string,
  resultId: number,
  awardedPoints: number,
) {
  requireDemoTeacher();
  const database = readDatabase();
  const result = database.results.find(
    (candidate) =>
      candidate.teacherId === database.teacher.id &&
      candidate.sessionUuid === sessionUuid &&
      candidate.questionResults.some((question) => question.id === resultId),
  );
  const pending = result
    ? database.pending.find((answer) => answer.resultId === resultId)
    : undefined;
  if (!pending) {
    throw demoProblem(404, "review.not_found", "The review item was not found.");
  }
  if (awardedPoints < 0 || awardedPoints > pending.points) {
    throw demoProblem(
      400,
      "review.points_out_of_range",
      "The awarded points are outside the allowed range.",
    );
  }
  pending.awardedPoints = awardedPoints;
  pending.status = "GRADED";
  const questionResult = result?.questionResults.find(
    (question) => question.id === resultId,
  );
  if (result && questionResult) {
    const previous = questionResult.awardedPoints ?? 0;
    questionResult.awardedPoints = awardedPoints;
    questionResult.status = "GRADED";
    result.score += awardedPoints - previous;
  }
  writeDatabase(database);
  return clone(pending);
}
