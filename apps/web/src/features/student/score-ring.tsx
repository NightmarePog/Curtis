"use client";

import { motion, useReducedMotion } from "motion/react";
import { useId } from "react";

import { cn } from "@/lib/cn";

export interface ScoreRingProps {
  score: number;
  maxScore: number;
  provisional?: boolean;
  size?: "sm" | "lg";
  className?: string;
}

function clampPercentage(score: number, maxScore: number) {
  if (!Number.isFinite(score) || !Number.isFinite(maxScore) || maxScore <= 0) {
    return 0;
  }

  return Math.min(100, Math.max(0, (score / maxScore) * 100));
}

export function ScoreRing({
  className,
  maxScore,
  provisional = false,
  score,
  size = "lg",
}: ScoreRingProps) {
  const titleId = useId();
  const descriptionId = useId();
  const reduceMotion = useReducedMotion();
  const percentage = clampPercentage(score, maxScore);
  const roundedPercentage = Math.round(percentage);
  const radius = 42;
  const circumference = 2 * Math.PI * radius;
  const dashOffset = circumference * (1 - percentage / 100);
  const label = provisional ? "Průběžné skóre" : "Výsledné skóre";

  return (
    <div
      className={cn(
        "relative shrink-0",
        size === "lg" ? "size-40 sm:size-44" : "size-20",
        className,
      )}
    >
      <svg
        viewBox="0 0 100 100"
        className="size-full -rotate-90"
        role="img"
        aria-labelledby={`${titleId} ${descriptionId}`}
      >
        <title id={titleId}>
          {label}: {score} z {maxScore} bodů
        </title>
        <desc id={descriptionId}>{roundedPercentage} procent dostupných bodů.</desc>
        <circle
          cx="50"
          cy="50"
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth="8"
          className="text-border"
        />
        {size === "lg" ? (
          <motion.circle
            cx="50"
            cy="50"
            r={radius}
            fill="none"
            stroke="currentColor"
            strokeWidth="8"
            strokeLinecap="round"
            strokeDasharray={circumference}
            className="text-brand"
            initial={reduceMotion ? false : { strokeDashoffset: circumference }}
            animate={{ strokeDashoffset: dashOffset }}
            transition={{ duration: reduceMotion ? 0 : 0.6, ease: "easeOut" }}
          />
        ) : (
          <circle
            cx="50"
            cy="50"
            r={radius}
            fill="none"
            stroke="currentColor"
            strokeWidth="8"
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={dashOffset}
            className="text-brand"
          />
        )}
      </svg>
      <span
        aria-hidden="true"
        className={cn(
          "absolute inset-0 grid place-items-center font-mono font-semibold tabular-nums text-foreground",
          size === "lg" ? "text-3xl" : "text-sm",
        )}
      >
        {roundedPercentage}&nbsp;%
      </span>
    </div>
  );
}
