import { Link } from "react-router-dom";

type PageHeaderProps = {
  eyebrow: string;
  title: string;
  text: string;
  showBack?: boolean;
};

export function PageHeader({ eyebrow, title, text, showBack = true }: PageHeaderProps) {
  return (
    <div className="mb-8">
      {showBack && (
        <Link to="/fleet" className="mb-4 inline-block text-sm font-medium text-ink/50 hover:text-ink">
          ← Back to dispatch desk
        </Link>
      )}
      <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">{eyebrow}</p>
      <h1 className="font-display text-4xl font-bold tracking-tight sm:text-5xl">{title}</h1>
      <p className="mt-3 max-w-xl text-ink/55">{text}</p>
    </div>
  );
}