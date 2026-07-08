interface LogoMarkProps {
  className?: string;
}

/**
 * Brand mark for Sauda — a rounded gradient tile with a two-way exchange glyph,
 * signalling supply/demand matching. Replaces the plain "S" placeholder box.
 */
export function LogoMark({ className = "h-9 w-9" }: LogoMarkProps) {
  return (
    <span
      className={`relative inline-flex shrink-0 items-center justify-center overflow-hidden rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-sm ring-1 ring-inset ring-white/15 ${className}`}
    >
      <span className="pointer-events-none absolute inset-x-0 top-0 h-1/2 bg-white/15" />
      <svg
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
        className="relative h-[58%] w-[58%]"
      >
        <path
          d="M6 9h9a2.5 2.5 0 0 1 2.5 2.5"
          stroke="currentColor"
          strokeWidth="2.1"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M9 6 6 9l3 3"
          stroke="currentColor"
          strokeWidth="2.1"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M18 15h-9A2.5 2.5 0 0 1 6.5 12.5"
          stroke="currentColor"
          strokeWidth="2.1"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <path
          d="M15 18l3-3-3-3"
          stroke="currentColor"
          strokeWidth="2.1"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </span>
  );
}

interface LogoProps {
  /** Optional label after the wordmark, e.g. "Admin". */
  suffix?: string;
  markClassName?: string;
  wordClassName?: string;
  className?: string;
}

export function Logo({
  suffix,
  markClassName = "h-9 w-9",
  wordClassName = "text-xl",
  className = "",
}: LogoProps) {
  return (
    <span className={`flex items-center gap-2.5 ${className}`}>
      <LogoMark className={markClassName} />
      <span className={`font-bold tracking-tight text-slate-900 ${wordClassName}`}>
        Sauda
        {suffix && <span className="font-semibold text-slate-400"> {suffix}</span>}
      </span>
    </span>
  );
}
