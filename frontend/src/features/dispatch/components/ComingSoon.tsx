type ComingSoonProps = {
  heading: string;
  items: string[];
};

export function ComingSoon({ heading, items }: ComingSoonProps) {
  return (
    <section className="rounded-2xl border border-dashed border-ink/20 bg-white p-6">
      <span className="rounded-full bg-mint px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-ink">
        Coming soon
      </span>
      <h2 className="mt-4 font-display text-2xl">{heading}</h2>
      <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-ink/60">
        {items.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>
    </section>
  );
}