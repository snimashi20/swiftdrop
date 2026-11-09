import { forwardRef, type InputHTMLAttributes } from "react";

type FormFieldProps = InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string };

export const FormField = forwardRef<HTMLInputElement, FormFieldProps>(function FormField(
  { label, error, ...props },
  ref,
) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-semibold text-ink/80">{label}</span>
      <input
        {...props}
        ref={ref}
        className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm outline-none transition placeholder:text-ink/30 focus:border-coral focus:ring-4 focus:ring-coral/10"
      />
      {error && <span className="text-xs text-coral">{error}</span>}
    </label>
  );
});
