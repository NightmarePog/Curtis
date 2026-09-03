/*
 * Compile-time checks between the Spring-generated OpenAPI models and the
 * frontend's explicit wire types. This module emits no runtime code.
 */
import type {
  AnswerRequest,
  AttemptDetailResponse,
  AttemptSummaryResponse,
  AttemptResponse,
  ClassroomResponse,
  GroupResponse,
  MeResponse,
  OptionResponse,
  PairResponse,
  QuestionResponse,
  QuestionResultResponse,
  QuizResponse,
  QuizWriteRequest,
  RankingResponse,
  ReviewResponse,
  SessionResponse,
  StudentQuestionResponse,
  SubjectResponse,
  TargetResponse,
  TeacherClassStudentsResponse,
  UserResponse,
  UserSummary,
} from "@/generated/api/models";
import type {
  ApiAdminUser,
  ApiAttempt,
  ApiAttemptDetail,
  ApiAttemptSummary,
  ApiClassGroup,
  ApiClassroom,
  ApiMe,
  ApiQuestionResult,
  ApiQuiz,
  ApiQuizOption,
  ApiQuizPair,
  ApiQuizQuestion,
  ApiQuizWrite,
  ApiRanking,
  ApiReview,
  ApiSession,
  ApiStudentQuestion,
  ApiSubject,
  ApiTarget,
  ApiTeacherClassStudents,
  ApiUserSummary,
} from "@/types/domain";

type DeepRequired<T> = T extends readonly (infer Item)[]
  ? DeepRequired<Item>[]
  : T extends object
    ? { [Key in keyof T]-?: DeepRequired<Exclude<T[Key], null | undefined>> }
    : Exclude<T, null | undefined>;

type SameKeys<Left, Right> =
  Exclude<keyof Left, keyof Right> extends never
    ? Exclude<keyof Right, keyof Left> extends never
      ? true
      : false
    : false;

type ContractMatches<Frontend, Backend> = SameKeys<Frontend, Backend> extends true
  ? DeepRequired<Backend> extends DeepRequired<Frontend>
    ? true
    : false
  : false;

type Assert<T extends true> = T;

type Contracts = [
  Assert<ContractMatches<ApiMe, MeResponse>>,
  Assert<ContractMatches<ApiUserSummary, UserSummary>>,
  Assert<ContractMatches<ApiAdminUser, UserResponse>>,
  Assert<ContractMatches<ApiClassGroup, GroupResponse>>,
  Assert<ContractMatches<ApiClassroom, ClassroomResponse>>,
  Assert<ContractMatches<ApiSubject, SubjectResponse>>,
  Assert<ContractMatches<ApiQuizOption, OptionResponse>>,
  Assert<ContractMatches<ApiQuizPair, PairResponse>>,
  Assert<ContractMatches<ApiQuizQuestion, QuestionResponse>>,
  Assert<ContractMatches<ApiQuiz, QuizResponse>>,
  Assert<ContractMatches<ApiQuizWrite, QuizWriteRequest>>,
  Assert<ContractMatches<ApiTarget, TargetResponse>>,
  Assert<ContractMatches<ApiSession, SessionResponse>>,
  Assert<ContractMatches<ApiStudentQuestion, StudentQuestionResponse>>,
  Assert<ContractMatches<ApiAttempt, AttemptResponse>>,
  Assert<ContractMatches<ApiAttemptSummary, AttemptSummaryResponse>>,
  Assert<ContractMatches<ApiQuestionResult, QuestionResultResponse>>,
  Assert<ContractMatches<ApiAttemptDetail, AttemptDetailResponse>>,
  Assert<ContractMatches<ApiReview, ReviewResponse>>,
  Assert<ContractMatches<ApiRanking, RankingResponse>>,
  Assert<ContractMatches<ApiTeacherClassStudents, TeacherClassStudentsResponse>>,
];

// Keep the tuple used so strict unused-type tooling cannot discard the checks.
export type ApiContractsVerified = Contracts;

// Request bodies that do not have a named frontend wire type are still exposed
// here for service-layer migration and editor discovery.
export type GeneratedAnswerRequest = AnswerRequest;
