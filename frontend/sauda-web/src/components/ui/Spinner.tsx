import { Loader2 } from "lucide-react";

interface SpinnerProps {
  className?: string;
  label?: string;
}

/** Inline spinner. Pass a label to render a centered "loading" row. */
export function Spinner({ className = "h-4 w-4", label }: SpinnerProps) {
  if (label) {
    return (
      <div
        role="status"
        className="flex items-center justify-center gap-2 py-8 text-sm text-slate-500"
      >
        <Loader2 className={`${className} animate-spin text-brand-600`} />
        <span>{label}</span>
      </div>
    );
  }
  return <Loader2 className={`${className} animate-spin`} aria-hidden="true" />;
}
