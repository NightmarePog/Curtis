"use client";

import {
  autocompletion,
  type Completion,
  type CompletionContext,
  type CompletionResult,
} from "@codemirror/autocomplete";
import { yaml } from "@codemirror/lang-yaml";
import { lintGutter, linter, type Diagnostic } from "@codemirror/lint";
import { oneDark } from "@codemirror/theme-one-dark";
import CodeMirror, {
  EditorView,
  type ReactCodeMirrorRef,
} from "@uiw/react-codemirror";
import { load, YAMLException } from "js-yaml";
import { useTheme } from "next-themes";
import {
  forwardRef,
  useImperativeHandle,
  useMemo,
  useRef,
  useSyncExternalStore,
} from "react";

export interface YamlCodeEditorHandle {
  focus: () => void;
}

interface YamlCodeEditorProps {
  describedBy: string;
  invalid: boolean;
  onBlur: () => void;
  onChange: (value: string) => void;
  value: string;
}

const subscribeToHydration = () => () => undefined;

const fieldCompletions: Completion[] = [
  { label: "title", apply: "title: ", type: "property", detail: "required", info: "Quiz title, up to 100 characters." },
  { label: "description", apply: "description: ", type: "property", detail: "optional", info: "A short description of the quiz." },
  { label: "subject", apply: "subject: ", type: "property", detail: "subject", info: "The exact name of a subject assigned to you." },
  { label: "chapter", apply: "chapter: ", type: "property", detail: "optional", info: "An optional chapter or topic." },
  { label: "maxQuestionsPerSession", apply: "maxQuestionsPerSession: ", type: "property", detail: "integer", info: "Maximum number of questions used in one session." },
  { label: "shuffle", apply: "shuffle: ", type: "property", detail: "boolean", info: "Whether questions should be shuffled." },
  { label: "questions", apply: "questions:\n  - question: ", type: "property", detail: "list", info: "The quiz question list." },
  { label: "question", apply: "question: ", type: "property", detail: "required", info: "The question prompt shown to students." },
  { label: "type", apply: "type: ", type: "property", detail: "question type", info: "MULTIPLE_CHOICE, MATCHING, or FREE_TEXT." },
  { label: "points", apply: "points: ", type: "property", detail: "integer", info: "Points awarded; defaults to 1." },
  { label: "timeInSeconds", apply: "timeInSeconds: ", type: "property", detail: "seconds", info: "A positive time limit for the question." },
  { label: "options", apply: "options:\n  - ", type: "property", detail: "list", info: "Answer choices for a multiple-choice question." },
  { label: "correctIndexes", apply: "correctIndexes: []", type: "property", detail: "indexes", info: "Indexes of correct options, starting at 0." },
  { label: "pairs", apply: "pairs:\n  - left: \n    right: ", type: "property", detail: "pair list", info: "Pairs used by a MATCHING question." },
  { label: "left", apply: "left: ", type: "property", detail: "left value" },
  { label: "right", apply: "right: ", type: "property", detail: "right value" },
  { label: "codeSnippet", apply: "codeSnippet: |\n  ", type: "property", detail: "code", info: "A multiline code sample displayed with the question." },
  { label: "imageRef", apply: "imageRef: ", type: "property", detail: "file name", info: "The exact name of an attached image, without a path." },
];

const questionTypes: Completion[] = [
  { label: "MULTIPLE_CHOICE", type: "enum", info: "Uses options and zero-based correctIndexes." },
  { label: "MATCHING", type: "enum", info: "Uses left and right values inside pairs." },
  { label: "FREE_TEXT", type: "enum", info: "A free-form response graded by the teacher." },
];

const booleanValues: Completion[] = [
  { label: "true", type: "keyword" },
  { label: "false", type: "keyword" },
];

function yamlCompletions(context: CompletionContext): CompletionResult | null {
  const word = context.matchBefore(/[\w]*/);
  if (!word || (word.from === word.to && !context.explicit)) return null;

  const line = context.state.doc.lineAt(context.pos);
  const beforeCursor = context.state.doc.sliceString(line.from, context.pos);
  const key = beforeCursor.match(/^\s*(?:-\s*)?([\w]+):\s*[\w]*$/)?.[1];
  const options = key === "type"
    ? questionTypes
    : key === "shuffle"
      ? booleanValues
      : fieldCompletions;

  return { from: word.from, options, validFor: /^[\w]*$/ };
}

export const YamlCodeEditor = forwardRef<
  YamlCodeEditorHandle,
  YamlCodeEditorProps
>(function YamlCodeEditor(
  { describedBy, invalid, onBlur, onChange, value },
  forwardedRef,
) {
  const editorRef = useRef<ReactCodeMirrorRef>(null);
  const { resolvedTheme } = useTheme();
  const hydrated = useSyncExternalStore(
    subscribeToHydration,
    () => true,
    () => false,
  );
  const isDark = hydrated ? resolvedTheme !== "light" : true;

  useImperativeHandle(forwardedRef, () => ({
    focus: () => editorRef.current?.view?.focus(),
  }));

  const extensions = useMemo(
    () => [
      yaml(),
      autocompletion({ override: [yamlCompletions] }),
      lintGutter(),
      EditorView.contentAttributes.of({
        "aria-describedby": describedBy,
        "aria-invalid": invalid ? "true" : "false",
        "aria-label": "Quiz YAML definition",
        spellcheck: "false",
      }),
      linter((view): Diagnostic[] => {
        const source = view.state.doc.toString();
        if (!source.trim()) return [];

        try {
          load(source);
          return [];
        } catch (error) {
          if (!(error instanceof YAMLException)) return [];
          const position = Math.min(
            Math.max(error.mark?.position ?? 0, 0),
            view.state.doc.length,
          );
          const line = view.state.doc.lineAt(position);

          return [
            {
              from: position,
              to: Math.min(Math.max(position + 1, line.to), view.state.doc.length),
              severity: "error",
              message: error.reason || "The YAML could not be parsed.",
            },
          ];
        }
      }),
    ],
    [describedBy, invalid],
  );

  return (
    <div
      className={[
        "yaml-code-editor overflow-hidden rounded-md border bg-field",
        "transition-[border-color,box-shadow] duration-150 motion-reduce:transition-none",
        invalid ? "border-danger" : "border-border-strong",
      ].join(" ")}
    >
      <CodeMirror
        ref={editorRef}
        value={value}
        height="34rem"
        minHeight="22rem"
        theme={isDark ? oneDark : "light"}
        extensions={extensions}
        basicSetup={{
          allowMultipleSelections: true,
          autocompletion: false,
          bracketMatching: true,
          closeBrackets: true,
          foldGutter: true,
          highlightActiveLine: true,
          highlightActiveLineGutter: true,
          highlightSelectionMatches: true,
          lineNumbers: true,
          searchKeymap: true,
          syntaxHighlighting: true,
        }}
        onBlur={onBlur}
        onChange={onChange}
      />
    </div>
  );
});
