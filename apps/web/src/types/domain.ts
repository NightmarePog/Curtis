export type Role = "ADMINISTRATOR" | "TEACHER" | "STUDENT" | (string & {});

export interface Me {
  sub: string;
  name: string;
  roles: Role[];
  id?: string;
  subject?: string;
  username?: string;
  displayName?: string;
}

export type QuestionType = "MULTIPLE_CHOICE" | "MATCHING" | "FREE_TEXT";
export type QuizStatus = "DRAFT" | "RUNNING" | "ARCHIVED";
export type ApiQuizStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

export interface Answer {
  answer: string | null;
  isCorrect: boolean | null;
  backendId?: string;
}

export interface MatchingPair {
  left: string;
  right: string;
  backendId?: string;
  leftId?: string;
  rightId?: string;
}

export interface Question {
  id?: number | string;
  question: string;
  type: QuestionType;
  points: number;
  codeSnippet: string | null;
  imageRef: string | null;
  answers: Answer[];
  pairs: MatchingPair[];
  timeInSeconds: number;
  quizUuid: string;
  backendId?: string;
}

export interface Quiz {
  uuid: string;
  title: string;
  description: string | null;
  subject: string | null;
  subjectUuid?: string | null;
  chapter: string | null;
  questions: Question[];
  maxQuestionsPerSession: number;
  shuffle: boolean;
  status: QuizStatus | null;
  createdAt: string | null;
  editedAt: string | null;
  validFrom: string | null;
  validTo: string | null;
  version?: number;
  creatorId?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface QuizInput {
  title: string;
  description?: string;
  subject?: string;
  subjectUuid?: string | null;
  chapter?: string;
  maxQuestionsPerSession: number;
  shuffle?: boolean;
  status?: QuizStatus;
  validFrom?: string;
  validTo?: string;
}

export interface QuestionInput {
  question: string;
  type: QuestionType;
  points: number;
  timeInSeconds: number;
  codeSnippet?: string;
  imageRef?: string;
  answers?: Answer[];
  pairs?: MatchingPair[];
}

export interface ActiveSession {
  sessionUuid: string;
  quizUuid: string;
  title: string;
  description: string | null;
  subject: string | null;
  chapter: string | null;
  teacherName: string;
  questionCount: number;
  startedAt: string;
  expiresAt: string;
  openToAllStudents: boolean;
  assignedClasses: AssignedClass[];
  status?: ApiSessionStatus;
}

export interface AssignedClass {
  uuid: string;
  name: string;
}

export interface ClassMember {
  studentId: string;
  studentName: string;
  preferredUsername?: string | null;
}

export interface ClassGroup {
  uuid: string;
  name: string;
  active: boolean;
  version: number;
  members: ClassMember[];
}

export interface ClassResponse {
  uuid: string;
  name: string;
  studentCount: number;
  members: ClassMember[];
  createdAt: string;
  updatedAt: string;
  active?: boolean;
  version?: number;
  groups?: ClassGroup[];
}

export interface ClassInput {
  name: string;
  studentIds?: string[];
}

export interface AdminTeacherSummary {
  teacherId: string;
  displayName: string;
  preferredUsername: string | null;
}

export interface AdminTeacherResponse extends AdminTeacherSummary {
  lastSeenAt: string | null;
}

export interface AdminClassResponse {
  uuid: string;
  name: string;
  teacherCount: number;
  teachers: AdminTeacherSummary[];
  studentCount: number;
  members: ClassMember[];
  createdAt: string;
  updatedAt: string;
  active?: boolean;
  version?: number;
  groups?: ClassGroup[];
}

export interface AdminClassInput {
  name: string;
  teacherIds: string[];
  studentIds?: string[];
}

export interface AssignedSubject {
  uuid: string;
  name: string;
}

export interface AdminSubjectResponse extends AssignedSubject {
  teacherCount: number;
  teachers: AdminTeacherSummary[];
  createdAt: string;
  updatedAt: string;
  code?: string;
  active?: boolean;
  version?: number;
}

export interface AdminSubjectInput {
  name: string;
  teacherIds?: string[];
}

export type QuestionSubmission =
  | { type: "MULTIPLE_CHOICE"; selectedIndexes: number[] }
  | {
      type: "MATCHING";
      pairs: Array<{ leftIndex: number; rightIndex: number }>;
    }
  | { type: "FREE_TEXT"; text: string };

export interface FinishQuestion {
  question: string;
  type: QuestionType;
  points: number;
  codeSnippet: string | null;
  imageRef: string | null;
  answers: Answer[];
  pairs: MatchingPair[];
}

export interface FinishResult {
  score: number | null;
  maxScore: number;
  pendingReviewCount: number;
  questions: FinishQuestion[];
}

export type ReviewStatus = "PENDING_REVIEW" | "GRADED";

export interface StoredQuestionResult {
  id: number | string;
  questionIndex: number;
  question: string;
  type: QuestionType;
  points: number;
  awardedPoints: number | null;
  status: ReviewStatus;
  text: string | null;
  selectedIndexes: number[];
  pairs: Array<{ leftIndex: number; rightIndex: number }>;
  optionLabels?: string[];
  matchingPairLabels?: Array<{ left: string; right: string }>;
}

export interface QuizResult {
  id: number | string;
  sessionUuid: string;
  quizUuid: string;
  studentId: string;
  studentName?: string | null;
  score: number;
  maxScore: number;
  playedAt: string;
  questionResults: StoredQuestionResult[];
  quizTitle?: string;
}

export interface PendingTextAnswer {
  resultId: number | string;
  studentId: string;
  studentName?: string | null;
  questionIndex: number;
  question: string;
  text: string;
  points: number;
  awardedPoints: number | null;
  status: ReviewStatus;
}

export interface TeacherAttemptSummary {
  resultId: number | string;
  sessionUuid: string;
  quizUuid: string;
  quizTitle: string;
  studentId: string;
  studentName: string | null;
  score: number;
  maxScore: number;
  percentage: number;
  playedAt: string;
  questionCount: number;
  pendingReviewCount: number;
}

export interface TeacherStudentSummary {
  studentId: string;
  studentName: string | null;
  attemptCount: number;
  totalScore: number;
  totalMaxScore: number;
  percentage: number;
  lastPlayedAt: string;
  attempts: TeacherAttemptSummary[];
}

export interface ClassLeaderboardMember {
  studentName: string;
  totalScore: number;
  totalMaxScore: number;
  percentage: number;
  attemptCount: number;
  rank: number;
  currentStudent: boolean;
}

export interface ClassLeaderboard {
  classUuid: string;
  className: string;
  members: ClassLeaderboardMember[];
}

export interface ApiErrorEnvelope {
  statusCode?: number;
  error?: string;
  message?: string | Record<string, string> | null;
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  traceId?: string;
  fieldErrors?: Record<string, string> | Array<{ field: string; message: string }>;
}

/* Production API wire contracts. Presentation adapters in services.ts keep the
 * Faker demo independent from backend DTO churn. */
export interface ApiMe {
  id: string;
  subject: string;
  username: string;
  displayName: string;
  roles: Role[];
}

export interface ApiUserSummary {
  id: string;
  displayName: string;
  username: string;
  roles: Role[];
}

export interface ApiAdminUser extends ApiUserSummary {
  active: boolean;
  firstLoginAt: string;
  lastLoginAt: string;
  version: number;
}

export interface ApiClassGroup {
  id: string;
  name: string;
  active: boolean;
  version: number;
  students: ApiUserSummary[];
}

export interface ApiClassroom {
  id: string;
  name: string;
  active: boolean;
  version: number;
  teachers: ApiUserSummary[];
  students: ApiUserSummary[];
  groups: ApiClassGroup[];
}

export interface ApiSubject {
  id: string;
  code: string;
  name: string;
  active: boolean;
  version: number;
  teachers?: ApiUserSummary[];
}

export interface ApiQuizOption {
  id: string;
  position: number;
  text: string;
  correct: boolean;
}

export interface ApiQuizPair {
  id: string;
  position: number;
  left: string;
  right: string;
}

export interface ApiQuizQuestion {
  id: string;
  position: number;
  type: QuestionType;
  prompt: string;
  points: number;
  codeSnippet: string | null;
  mediaId: string | null;
  timeSeconds: number;
  options: ApiQuizOption[];
  pairs: ApiQuizPair[];
}

export interface ApiQuiz {
  id: string;
  version: number;
  creatorId: string;
  subjectId: string;
  subjectName: string;
  title: string;
  description: string | null;
  chapter: string | null;
  status: ApiQuizStatus;
  maxQuestionsPerSession: number;
  shuffle: boolean;
  validFrom: string | null;
  validTo: string | null;
  createdAt: string;
  updatedAt: string;
  archivedAt: string | null;
  questions: ApiQuizQuestion[];
}

export interface ApiQuizWrite {
  title: string;
  description: string | null;
  subjectId: string;
  chapter: string | null;
  status: ApiQuizStatus;
  maxQuestionsPerSession: number;
  shuffle: boolean;
  validFrom: string | null;
  validTo: string | null;
  expectedVersion: number | null;
  questions: Array<{
    id: string | null;
    type: QuestionType;
    prompt: string;
    points: number;
    codeSnippet: string | null;
    mediaId: string | null;
    timeSeconds: number;
    options: Array<{ id: string | null; text: string; correct: boolean }>;
    pairs: Array<{ id: string | null; left: string; right: string }>;
  }>;
}

export type ApiSessionStatus = "ACTIVE" | "CLOSED" | "EXPIRED";
export type ApiScorePolicy = "BEST" | "LATEST" | "ALL";
export type ApiAttemptStatus =
  | "IN_PROGRESS"
  | "SUBMITTED"
  | "PENDING_REVIEW"
  | "GRADED"
  | "EXPIRED";

export interface ApiTarget {
  id: string;
  classId: string;
  name: string;
  type: "CLASS" | "GROUP" | string;
}

export interface ApiSession {
  id: string;
  quizId: string;
  title: string;
  description: string | null;
  subject: string | null;
  chapter: string | null;
  teacherName: string;
  status: ApiSessionStatus;
  startedAt: string;
  closesAt: string;
  questionCount: number;
  maxAttempts: number;
  scorePolicy: ApiScorePolicy;
  targets: ApiTarget[];
}

export interface ApiStudentQuestion {
  id: string;
  position: number;
  type: QuestionType;
  prompt: string;
  points: number;
  codeSnippet: string | null;
  mediaId: string | null;
  deadlineAt: string | null;
  options: Array<{ id: string; text: string }>;
  leftItems: Array<{ id: string; text: string }>;
  rightItems: Array<{ id: string; text: string }>;
}

export interface ApiAttempt {
  id: string;
  sessionId: string;
  attemptNumber: number;
  status: ApiAttemptStatus;
  answeredQuestions: number;
  totalQuestions: number;
  score: number | null;
  maxScore: number;
  pendingReviewCount: number;
  startedAt: string;
  submittedAt: string | null;
  currentQuestion: ApiStudentQuestion | null;
  readyToSubmit: boolean;
}

export interface ApiAttemptSummary {
  id: string;
  sessionId: string;
  sessionTitle: string;
  studentId: string;
  studentName: string;
  attemptNumber: number;
  status: ApiAttemptStatus;
  score: number;
  maxScore: number;
  percentage: number;
  pendingReviewCount: number;
  startedAt: string;
  submittedAt: string | null;
}

export interface ApiQuestionResult {
  id: string;
  position: number;
  prompt: string;
  type: QuestionType;
  maxPoints: number;
  awardedPoints: number | null;
  state: string;
  gradingState: "AUTO_GRADED" | "PENDING_REVIEW" | "MANUALLY_GRADED";
  options: Array<{ id: string; text: string }>;
  matchingPairs: Array<{
    leftId: string;
    left: string;
    rightId: string;
    right: string;
  }>;
  response: unknown;
  correctAnswer: unknown;
  answeredAt: string | null;
}

export interface ApiAttemptDetail {
  attempt: ApiAttemptSummary;
  questions: ApiQuestionResult[];
}

export interface ApiReview {
  questionResultId: string;
  attemptId: string;
  sessionId: string;
  sessionTitle: string;
  studentId: string;
  studentName: string;
  prompt: string;
  answer: string;
  maxPoints: number;
  awardedPoints: number | null;
  answeredAt: string | null;
}

export interface ApiRanking {
  type: "CLASS" | "GROUP" | string;
  id: string;
  classId: string;
  name: string;
  members: Array<{
    displayName: string;
    score: number;
    maxScore: number;
    percentage: number;
    attemptCount: number;
    rank: number;
    currentStudent: boolean;
  }>;
}

export interface ApiTeacherClassStudents {
  classId: string;
  className: string;
  students: Array<{
    id: string;
    displayName: string;
    groups: ApiTarget[];
    attemptCount: number;
    score: number;
    maxScore: number;
    percentage: number;
    lastActivity: string | null;
  }>;
}
