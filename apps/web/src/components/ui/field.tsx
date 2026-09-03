import {
  cloneElement,
  isValidElement,
  useId,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
} from "react";
import { CircleAlert } from "lucide-react";

import { cn } from "@/lib/utils";

interface FieldControlProps {
  id?: string;
  required?: boolean;
  "aria-describedby"?: string;
  "aria-invalid"?: boolean | "false" | "true";
}

export interface FieldProps
  extends Omit<HTMLAttributes<HTMLDivElement>, "children"> {
  children: ReactElement<FieldControlProps>;
  controlId?: string;
  description?: ReactNode;
  error?: ReactNode;
  label: ReactNode;
  optional?: boolean;
  required?: boolean;
}

export function Field({
  children,
  className,
  controlId,
  description,
  error,
  label,
  optional = false,
  required = false,
  ...props
}: FieldProps) {
  const generatedId = useId().replaceAll(":", "");
  const child = isValidElement<FieldControlProps>(children) ? children : null;
  const fieldId = controlId || child?.props.id || `field-${generatedId}`;
  const descriptionId = description ? `${fieldId}-description` : undefined;
  const errorId = error ? `${fieldId}-error` : undefined;
  const describedBy = [
    child?.props["aria-describedby"],
    descriptionId,
    errorId,
  ]
    .filter(Boolean)
    .join(" ");

  const control = child
    ? cloneElement(child, {
        id: fieldId,
        required: child.props.required ?? required,
        "aria-describedby": describedBy || undefined,
        "aria-invalid": error ? true : child.props["aria-invalid"],
      })
    : children;

  return (
    <div data-slot="field" className={cn("grid gap-2", className)} {...props}>
      <label
        htmlFor={fieldId}
        data-slot="field-label"
        className="w-fit text-sm leading-5 font-semibold text-foreground"
      >
        {label}
        {required ? (
          <span className="ml-1 text-danger-text">
            <span aria-hidden="true">*</span>
            <span className="sr-only"> (povinné)</span>
          </span>
        ) : optional ? (
          <span className="ml-1 font-normal text-muted-foreground">(volitelné)</span>
        ) : null}
      </label>
      {control}
      {description ? (
        <p
          id={descriptionId}
          data-slot="field-description"
          className="text-sm leading-5 text-muted-foreground"
        >
          {description}
        </p>
      ) : null}
      {error ? (
        <p
          id={errorId}
          role="alert"
          data-slot="field-error"
          className="flex items-start gap-1.5 text-sm leading-5 text-danger-text"
        >
          <CircleAlert
            aria-hidden="true"
            className="mt-0.5 size-4 shrink-0"
            strokeWidth={2}
          />
          <span>{error}</span>
        </p>
      ) : null}
    </div>
  );
}
