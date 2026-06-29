import type { ReactNode } from "react";

type Tone = "green" | "orange" | "red" | "blue" | "gray";

interface BadgeProps {
  children: ReactNode;
  tone?: Tone;
  className?: string;
}

const tones: Record<Tone, string> = {
  green: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  orange: "bg-amber-50 text-amber-700 ring-amber-200",
  red: "bg-red-50 text-red-700 ring-red-200",
  blue: "bg-brand-50 text-brand-700 ring-brand-200",
  gray: "bg-slate-100 text-slate-600 ring-slate-200",
};

export function Badge({ children, tone = "gray", className = "" }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${tones[tone]} ${className}`}
    >
      {children}
    </span>
  );
}

export function MatchScoreBadge({ percent }: { percent: number | null }) {
  if (percent == null) return <Badge tone="gray">—</Badge>;
  const tone: Tone = percent >= 80 ? "green" : percent >= 50 ? "orange" : "red";
  return <Badge tone={tone}>{percent}%</Badge>;
}
