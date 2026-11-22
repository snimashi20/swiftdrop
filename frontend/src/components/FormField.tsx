import { forwardRef, type InputHTMLAttributes } from "react";

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export const FormField = forwardRef<HTMLInputElement, FormFieldProps>(
  ({ label, error, className, ...props }, ref) => (
    <label className="block space-y-1.5">
      <span className="text-sm font-semibold text-ink/80">{label}</span>
      <input
        {...props}
        ref={ref}
        className={`w-full rounded-xl border bg-white px-3.5 py-3 text-sm outline-none transition placeholder:text-ink/30 ${
          error
            ? "border-rose-500 focus:border-rose-500 focus:ring-4 focus:ring-rose-500/15"
            : "border-ink/15 focus:border-coral focus:ring-4 focus:ring-coral/10"
        } ${className ?? ""}`}
      />
      {error && (
        <span className="block mt-1 text-xs font-semibold text-rose-600">
          {error}
        </span>
      )}
    </label>
  )
);

FormField.displayName = "FormField";