import { useEffect } from "react";
import type { ReactNode } from "react";

type ModalProps = {
  title: string;
  onClose: () => void;
  children: ReactNode;
};

export function Modal({ title, onClose, children }: ModalProps) {
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center sm:items-center">
      <button className="absolute inset-0 bg-ink/40" aria-label="Close dialog" onClick={onClose} />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="relative z-50 max-h-[90vh] w-full overflow-y-auto rounded-t-3xl bg-white p-6 shadow-xl sm:max-w-lg sm:rounded-3xl"
      >
        <div className="mb-4 flex items-start justify-between gap-4">
          <h2 className="font-display text-2xl font-bold">{title}</h2>
          <button onClick={onClose} className="rounded-lg px-2 py-1 text-ink/50 hover:bg-ink hover:text-white transition" aria-label="Close">
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}