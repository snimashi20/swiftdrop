import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

/** The look of the admin screens: eyebrow label, serif title, a divider, then the content. */
type SectionCardProps = {
  eyebrow: string;
  title: string;
  description?: string;
  aside?: ReactNode;
  children: ReactNode;
};

export function SectionCard({ eyebrow, title, description, aside, children }: SectionCardProps) {
  return (
    <section className="mb-8 rounded-3xl border border-ink/10 bg-white p-6 sm:p-8">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-ink/10 pb-6">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">{eyebrow}</p>
          <h2 className="mt-2 font-display text-3xl font-bold">{title}</h2>
          {description && <p className="mt-1 text-sm text-ink/55">{description}</p>}
        </div>
        {aside}
      </div>
      <div className="pt-6">{children}</div>
    </section>
  );
}

type FieldProps = {
  label: string;
  icon: LucideIcon;
  hint?: string;
  children: ReactNode;
};

/** Uppercase label with an icon, the input, and a small hint underneath. */
export function Field({ label, icon: Icon, hint, children }: FieldProps) {
  return (
    <label className="block">
      <span className="mb-2 flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-ink/70">
        <Icon size={14} className="text-coral" />
        {label}
      </span>
      {children}
      {hint && <span className="mt-2 block text-xs text-ink/45">{hint}</span>}
    </label>
  );
}

export const fieldClass =
  "w-full rounded-2xl border border-ink/15 bg-white px-4 py-3.5 text-sm outline-none transition placeholder:text-ink/30 focus:border-coral focus:ring-4 focus:ring-coral/10";

export const ghostButtonClass =
  "inline-flex items-center gap-2 rounded-2xl border border-ink/10 bg-white px-5 py-3 text-sm font-semibold transition hover:bg-ink hover:text-white disabled:opacity-50";

export const primaryButtonClass =
  "inline-flex items-center justify-center gap-2 rounded-2xl bg-coral px-6 py-3.5 font-semibold text-white shadow-lg shadow-coral/20 transition hover:bg-ink disabled:opacity-60";