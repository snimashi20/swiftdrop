import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { Link } from "react-router-dom";

type Badge = { label: string; tone?: "coral" | "mint" };

type PageHeaderProps = {
  eyebrow: string;
  title: string;
  text: string;
  showBack?: boolean;
  icon?: LucideIcon;
  badges?: Badge[];
  actions?: ReactNode;
};

export function PageHeader({ eyebrow, title, text, showBack = true, icon: Icon, badges = [], actions }: PageHeaderProps) {
  return (
    <div className="mb-8">
      {showBack && (
        <Link to="/fleet" className="mb-4 inline-block text-sm font-medium text-ink/50 hover:text-ink">
          ← Back to dispatch desk
        </Link>
      )}
      <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-5">
          {Icon && (
            <div className="grid h-20 w-20 shrink-0 place-items-center rounded-3xl border border-coral/30 bg-coral/10 text-coral sm:h-24 sm:w-24">
              <Icon size={34} />
            </div>
          )}
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">{eyebrow}</p>
              {badges.map((badge) => (
                <span
                  key={badge.label}
                  className={`rounded-md px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider ${
                    badge.tone === "mint" ? "bg-mint text-ink" : "bg-coral/10 text-coral"
                  }`}
                >
                  {badge.label}
                </span>
              ))}
            </div>
            <h1 className="mt-2 font-display text-4xl font-bold tracking-tight sm:text-5xl">{title}</h1>
            <p className="mt-2 max-w-xl text-ink/55">{text}</p>
          </div>
        </div>
        {actions && <div className="flex shrink-0 flex-wrap items-center gap-3">{actions}</div>}
      </div>
    </div>
  );
}