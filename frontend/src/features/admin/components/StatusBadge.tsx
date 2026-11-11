import React from "react";
import { Clock } from "lucide-react";

interface StatusBadgeProps {
  enabled: boolean;
  status?: string;
  size?: "sm" | "md";
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  enabled,
  status,
  size = "md",
}) => {
  const sizeClasses =
    size === "sm"
      ? "px-2 py-0.5 text-[11px] gap-1.5"
      : "px-2.5 py-1 text-xs gap-2";

  if (status === "PENDING_APPROVAL") {
    return (
      <span
        className={`inline-flex items-center font-semibold rounded-full bg-amber-50 text-amber-800 border border-amber-300 shadow-xs ${sizeClasses}`}
      >
        <Clock size={12} className="text-amber-600 animate-pulse" />
        <span>Pending Approval</span>
      </span>
    );
  }

  if (status === "REJECTED") {
    return (
      <span
        className={`inline-flex items-center font-medium rounded-full bg-rose-50 text-rose-700 border border-rose-200/80 shadow-xs ${sizeClasses}`}
      >
        <span className="h-2 w-2 rounded-full bg-rose-400" />
        <span>Rejected</span>
      </span>
    );
  }

  return enabled ? (
    <span
      className={`inline-flex items-center font-medium rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200/80 shadow-xs ${sizeClasses}`}
    >
      <span className="relative flex h-2 w-2">
        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
        <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500" />
      </span>
      <span>Active</span>
    </span>
  ) : (
    <span
      className={`inline-flex items-center font-medium rounded-full bg-rose-50 text-rose-700 border border-rose-200/80 shadow-xs ${sizeClasses}`}
    >
      <span className="h-2 w-2 rounded-full bg-rose-400" />
      <span>Disabled</span>
    </span>
  );
};
