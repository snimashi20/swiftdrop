import React from "react";
import { Shield, Truck, Store, User, Radio } from "lucide-react";
import type { Role } from "../../auth/auth.types";

interface RoleBadgeProps {
  role: Role | string;
  size?: "sm" | "md";
}

export const RoleBadge: React.FC<RoleBadgeProps> = ({ role, size = "md" }) => {
  const normalizedRole = (role || "").toUpperCase();

  const getRoleConfig = () => {
    switch (normalizedRole) {
      case "ADMIN":
        return {
          label: "Admin",
          icon: Shield,
          styles: "bg-indigo-50 text-indigo-700 border-indigo-200/80 ring-1 ring-indigo-500/10",
        };
      case "DISPATCHER":
        return {
          label: "Dispatcher",
          icon: Radio,
          styles: "bg-sky-50 text-sky-700 border-sky-200/80 ring-1 ring-sky-500/10",
        };
      case "COURIER":
        return {
          label: "Courier",
          icon: Truck,
          styles: "bg-amber-50 text-amber-800 border-amber-200/80 ring-1 ring-amber-500/10",
        };
      case "MERCHANT":
        return {
          label: "Merchant",
          icon: Store,
          styles: "bg-emerald-50 text-emerald-800 border-emerald-200/80 ring-1 ring-emerald-500/10",
        };
      case "CUSTOMER":
      default:
        return {
          label: "Customer",
          icon: User,
          styles: "bg-slate-100 text-slate-700 border-slate-200 ring-1 ring-slate-400/10",
        };
    }
  };

  const { label, icon: Icon, styles } = getRoleConfig();
  const sizeClasses =
    size === "sm"
      ? "px-2 py-0.5 text-[11px] gap-1"
      : "px-2.5 py-1 text-xs gap-1.5";

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full border shadow-xs transition-colors ${sizeClasses} ${styles}`}
    >
      <Icon size={size === "sm" ? 11 : 13} className="shrink-0" />
      <span>{label}</span>
    </span>
  );
};
