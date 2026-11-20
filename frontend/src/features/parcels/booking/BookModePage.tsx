import { ArrowDown, ArrowRight, ArrowUp, Check, Package } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { DISTRICTS } from "../parcels.api";

// First page of booking: does the customer want to send or receive a parcel?
export function BookModePage() {
  const navigate = useNavigate();

  const choices = [
    {
      mode: "send",
      label: "Send",
      text: "Send a parcel to someone",
      icon: ArrowUp,
      tile: "bg-ink text-white",
      hover: "hover:border-ink/30",
      points: ["We collect it from your door", `Delivered across ${DISTRICTS.length} districts`, "Track every step live"],
    },
    {
      mode: "receive",
      label: "Receive",
      text: "Get a parcel picked up for you",
      icon: ArrowDown,
      tile: "bg-coral text-white",
      hover: "hover:border-coral/50",
      points: ["A courier collects it for you", "Your name and phone are added for you", "Track every step live"],
    },
  ] as const;

  return (
    <div className="mx-auto max-w-4xl">
      <div className="text-center">
        <p className="text-xs font-semibold uppercase tracking-wide text-coral">Book a delivery</p>
        <h1 className="mt-1 font-display text-4xl font-bold">I want to</h1>
        <p className="mx-auto mt-2 max-w-md text-ink/60">Select if you want to send or receive a parcel.</p>
      </div>

      <div className="mt-8 grid gap-4 md:grid-cols-2">
        {choices.map(({ mode, label, text, icon: Icon, tile, hover, points }) => (
          <button
            key={mode}
            type="button"
            onClick={() => navigate(`/parcels/new?mode=${mode}`)}
            className={`group flex flex-col rounded-2xl border border-ink/5 bg-white p-6 text-left shadow-sm transition hover:shadow-md ${hover}`}
          >
            <span className="flex items-center gap-4">
              <span className={`relative grid h-14 w-14 shrink-0 place-items-center rounded-2xl ${tile}`}>
                <Package className="h-7 w-7" strokeWidth={1.5} />
                <span className="absolute -right-2 -top-2 grid h-6 w-6 place-items-center rounded-full border-2 border-white bg-mint text-ink">
                  <Icon className="h-3.5 w-3.5" />
                </span>
              </span>
              <span>
                <span className="block font-display text-2xl font-bold">{label}</span>
                <span className="block text-sm text-ink/60">{text}</span>
              </span>
            </span>

            <ul className="mt-5 space-y-2 border-t border-ink/10 pt-4">
              {points.map((point) => (
                <li key={point} className="flex items-center gap-2 text-sm text-ink/80">
                  <Check className="h-4 w-4 shrink-0 text-coral" />
                  {point}
                </li>
              ))}
            </ul>

            <span className="mt-5 flex items-center gap-1 text-sm font-semibold text-coral">
              Continue
              <ArrowRight className="h-4 w-4 transition group-hover:translate-x-1" />
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}
