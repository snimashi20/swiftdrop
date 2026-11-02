interface Props {
  title: string;
  subtitle: string;
}

export default function PageHeader({ title, subtitle }: Props) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
        <p className="text-sm text-slate-600">{subtitle}</p>
      </div>
      <div className="flex items-center gap-3">
        {/* The real notification bell is M1's component; this is a placeholder. */}
        <button
          type="button"
          aria-label="Notifications"
          className="flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-600"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.7 21a2 2 0 0 1-3.4 0" />
          </svg>
        </button>
        <div className="hidden items-center gap-2 sm:flex">
          <span className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100 font-bold text-blue-800">G</span>
          <div className="leading-tight">
            <div className="text-sm font-semibold text-slate-900">Guest</div>
            <div className="text-xs text-slate-500">Public tracking</div>
          </div>
        </div>
      </div>
    </div>
  );
}
