import type {
  InputHTMLAttributes,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";

const fieldClass =
  "mt-1 w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100";

interface FieldProps {
  label: string;
  hint?: string;
  className?: string;
}

export function InputField({
  label,
  hint,
  className = "",
  ...props
}: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className={`block text-sm font-medium text-slate-700 ${className}`}>
      {label}
      <input className={fieldClass} {...props} />
      {hint && <span className="mt-1 block text-xs text-slate-400">{hint}</span>}
    </label>
  );
}

export function TextAreaField({
  label,
  hint,
  className = "",
  rows = 3,
  ...props
}: FieldProps & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <label className={`block text-sm font-medium text-slate-700 ${className}`}>
      {label}
      <textarea className={fieldClass} rows={rows} {...props} />
      {hint && <span className="mt-1 block text-xs text-slate-400">{hint}</span>}
    </label>
  );
}

export function SelectField({
  label,
  className = "",
  children,
  ...props
}: FieldProps & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <label className={`block text-sm font-medium text-slate-700 ${className}`}>
      {label}
      <select className={fieldClass} {...props}>
        {children}
      </select>
    </label>
  );
}
