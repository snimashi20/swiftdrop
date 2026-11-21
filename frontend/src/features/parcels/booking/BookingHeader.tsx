import { ArrowLeft } from "lucide-react";
import type { ReactNode } from "react";

// The same heading on every booking page: round back button, title and a short line under it.
export function BookingHeader({
  title,
  subtitle,
  onBack,
  action,
}: {
  title: string;
  subtitle?: string;
  onBack: () => void;
  action?: ReactNode;
}) {
  return (
    <div className="mb-5 flex items-center gap-3">
      <button
        type="button"
        aria-label="Back"
        onClick={onBack}
        className="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-ink/10 bg-white shadow-sm hover:bg-mist"
      >
        <ArrowLeft className="h-5 w-5" />
      </button>
      <div className="min-w-0 flex-1">
        <h1 className="font-display text-3xl font-bold">{title}</h1>
        {subtitle && <p className="text-sm text-ink/60">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}
