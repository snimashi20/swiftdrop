interface Props {
  title: string;
  subtitle: string;
}

export default function PageHeader({ title, subtitle }: Props) {
  return (
    <div>
      <h1 className="font-display text-3xl font-bold tracking-tight text-ink">{title}</h1>
      <p className="mt-1 text-sm text-ink/60">{subtitle}</p>
    </div>
  );
}
