export interface Me {
  sub: string;
  roles: string[];
}

export interface QuestionAnswer {
  isCorrect: boolean | null;
  answer: string;
}

export interface QuestionResponse {
  question: string;
  answers: QuestionAnswer[];
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
}

export interface Question {
  id: number;
  question: string;
  answers: QuestionAnswer[];
  timeInSeconds: number | null;
}

export interface QuestionResult {
  question: string;
  answers: QuestionAnswer[];
}

export interface ResultsResponse {
  score: number;
  maxScore: number;
  questions: QuestionResult[];
}

export interface QuizResult {
  id: number;
  sessionUuid: string;
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
}

export interface QuizPatchRequest {
  title?: string;
  description?: string | null;
  maxQuestionsPerSession?: number;
  shuffle?: boolean;
}

export interface QuestionCreateDto {
  question: string;
  answers: QuestionAnswer[];
  timeInSeconds: number;
}

export interface QuestionPatchDto {
  question?: string;
  answers?: QuestionAnswer[];
  timeInSeconds?: number;
}
