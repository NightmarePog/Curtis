export interface Me {
  sub: string;
  name?: string;
  roles: string[];
}

export interface QuestionAnswer {
  isCorrect: boolean | null;
  answer: string;
}

export type QuestionType = "MULTIPLE_CHOICE" | "MATCHING" | "FREE_TEXT";

export interface MatchingPair {
  left: string;
  right: string;
}

export interface QuestionResponse {
  question: string;
  type: QuestionType;
  points: number;
  codeSnippet: string | null;
  imageRef: string | null;
  answers: QuestionAnswer[];
  pairs: MatchingPair[];
  timeInSeconds: number | null;
  quizUuid: string | null;
}

export interface Quiz {
  uuid: string;
  title: string;
  description: string | null;
  questions: QuestionResponse[];
  maxQuestionsPerSession: number | null;
  shuffle: boolean | null;
  status: "DRAFT" | "RUNNING" | "ARCHIVED" | null;
  createdAt: string | null;
  editedAt: string | null;
  validFrom: string | null;
  validTo: string | null;
}

export interface Question {
  id: number;
  question: string;
  type: QuestionType;
  points: number;
  codeSnippet: string | null;
  imageRef: string | null;
  answers: QuestionAnswer[];
  pairs: MatchingPair[];
  timeInSeconds: number | null;
}

export interface QuestionResult {
  question: string;
  type: QuestionType;
  points: number;
  codeSnippet: string | null;
  imageRef: string | null;
  answers: QuestionAnswer[];
  pairs: MatchingPair[];
}

export interface ResultsResponse {
  score: number;
  maxScore: number;
  questions: QuestionResult[];
}

export interface QuizResult {
  id: number;
  sessionUuid: string;
  quizUuid: string;
  studentId: string;
  score: number;
  maxScore: number;
  playedAt: string;
}

export interface QuizCreateRequest {
  title: string;
  description?: string | null;
  maxQuestionsPerSession: number;
  shuffle: boolean;
  status?: "DRAFT" | "RUNNING" | "ARCHIVED";
  validFrom?: string | null;
  validTo?: string | null;
}

export interface QuizPatchRequest {
  title?: string;
  description?: string | null;
  maxQuestionsPerSession?: number;
  shuffle?: boolean;
  status?: "DRAFT" | "RUNNING" | "ARCHIVED";
  validFrom?: string | null;
  validTo?: string | null;
}

export interface QuestionCreateDto {
  question: string;
  type: QuestionType;
  points: number;
  codeSnippet?: string | null;
  imageRef?: string | null;
  answers: QuestionAnswer[];
  pairs: MatchingPair[];
  timeInSeconds: number;
}

export interface QuestionPatchDto {
  question?: string;
  type?: QuestionType;
  points?: number;
  codeSnippet?: string | null;
  imageRef?: string | null;
  answers?: QuestionAnswer[];
  pairs?: MatchingPair[];
  timeInSeconds?: number;
}

export interface MatchingSubmissionPair {
  leftIndex: number;
  rightIndex: number;
}

export type QuestionSubmission =
  | { type: "MULTIPLE_CHOICE"; selectedIndexes: number[] }
  | { type: "MATCHING"; pairs: MatchingSubmissionPair[] }
  | { type: "FREE_TEXT"; text: string };

export interface PendingTextAnswer {
  resultId: number;
  studentId: string;
  questionIndex: number;
  question: string;
  text: string;
  points: number;
  awardedPoints: number | null;
  status: "PENDING_REVIEW" | "GRADED" | string;
}
