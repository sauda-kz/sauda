import { AlertCircle, AlertTriangle } from "lucide-react";
import { useState } from "react";
import type { ParsedRowErrorItem } from "../types";

interface RowErrorsPopoverProps {
  errors: ParsedRowErrorItem[] | null | undefined;
  warnings: ParsedRowErrorItem[] | null | undefined;
}

export function RowErrorsPopover({ errors, warnings }: RowErrorsPopoverProps) {
  const [open, setOpen] = useState(false);
  const errorItems = errors ?? [];
  const warningItems = warnings ?? [];
  const count = errorItems.length + warningItems.length;

  if (count === 0) {
    return <span className="text-slate-300">—</span>;
  }

  return (
    <div className="relative inline-block">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-xs font-medium text-amber-700 hover:bg-amber-50"
        aria-expanded={open}
      >
        {errorItems.length > 0 ? (
          <AlertCircle className="h-4 w-4 text-red-500" />
        ) : (
          <AlertTriangle className="h-4 w-4 text-amber-500" />
        )}
        {count}
      </button>

      {open && (
        <>
          <button
            type="button"
            className="fixed inset-0 z-10 cursor-default"
            aria-label="Закрыть"
            onClick={() => setOpen(false)}
          />
          <div className="absolute right-0 z-20 mt-1 w-72 rounded-lg border border-slate-200 bg-white p-3 text-left shadow-lg">
            {errorItems.length > 0 && (
              <div className="space-y-2">
                <p className="text-xs font-semibold uppercase tracking-wide text-red-600">
                  Ошибки
                </p>
                <ul className="space-y-1.5">
                  {errorItems.map((item, index) => (
                    <li key={`e-${index}`} className="text-xs text-slate-700">
                      {item.field && (
                        <span className="font-medium text-slate-900">{item.field}: </span>
                      )}
                      {item.message}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {warningItems.length > 0 && (
              <div className={`space-y-2 ${errorItems.length > 0 ? "mt-3 border-t border-slate-100 pt-3" : ""}`}>
                <p className="text-xs font-semibold uppercase tracking-wide text-amber-600">
                  Предупреждения
                </p>
                <ul className="space-y-1.5">
                  {warningItems.map((item, index) => (
                    <li key={`w-${index}`} className="text-xs text-slate-700">
                      {item.field && (
                        <span className="font-medium text-slate-900">{item.field}: </span>
                      )}
                      {item.message}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
