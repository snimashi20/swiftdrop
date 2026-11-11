import { AlertCircle, CheckCircle2, Info, X, XCircle } from "lucide-react";
import type { ReactNode } from "react";

export type ToastVariant = "success" | "error" | "info" | "warning";

export type ToastProps = {
  title: string;
  message?: ReactNode;
  variant?: ToastVariant;
  onClose?: () => void;
  action?: ReactNode;
};

const variantStyles: Record<ToastVariant, { icon: typeof CheckCircle2; classes: string; live: "polite" | "assertive" }> = {
  success: { icon: CheckCircle2, classes: "border-mint bg-mint/60 text-ink", live: "polite" },
  error: { icon: XCircle, classes: "border-red-200 bg-red-50 text-red-950", live: "assertive" },
  info: { icon: Info, classes: "border-sky-200 bg-sky-50 text-sky-950", live: "polite" },
  warning: { icon: AlertCircle, classes: "border-amber-200 bg-amber-50 text-amber-950", live: "assertive" },
};

export function Toast({ title, message, variant = "info", onClose, action }: ToastProps) {
  const { icon: Icon, classes, live } = variantStyles[variant];
  return (
    <div role="status" aria-live={live} className={`flex w-full max-w-sm items-start gap-3 rounded-xl border p-4 shadow-lg ${classes}`}>
      <Icon size={20} className="mt-0.5 shrink-0" aria-hidden="true" />
      <div className="min-w-0 flex-1">
        <p className="font-semibold">{title}</p>
        {message && <div className="mt-1 text-sm opacity-80">{message}</div>}
        {action && <div className="mt-3">{action}</div>}
      </div>
      {onClose && <button type="button" onClick={onClose} className="rounded-md p-1 opacity-60 hover:bg-black/5 hover:opacity-100" aria-label="Dismiss notification"><X size={17} /></button>}
    </div>
  );
}

export function ToastViewport({ children }: { children: ReactNode }) {
  return <div className="pointer-events-none fixed inset-x-4 top-4 z-50 flex flex-col items-end gap-3 sm:left-auto sm:w-auto">{children}</div>;
}