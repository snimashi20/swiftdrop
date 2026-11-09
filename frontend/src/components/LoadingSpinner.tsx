import { LoaderCircle } from "lucide-react";

type LoadingSpinnerProps = {
  label?: string;
  size?: "sm" | "md" | "lg";
  className?: string;
};

const sizes = { sm: 16, md: 24, lg: 36 } as const;

export function LoadingSpinner({ label = "Loading", size = "md", className = "" }: LoadingSpinnerProps) {
  return (
    <span className={`inline-flex items-center gap-2 text-ink/60 ${className}`} role="status" aria-label={label}>
      <LoaderCircle size={sizes[size]} className="animate-spin" aria-hidden="true" />
      <span className="sr-only">{label}</span>
    </span>
  );
}