import { ArrowDown, ArrowUp, ChevronRight, Download } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { getApiErrorMessage } from "../../lib/api";
import { useAuth } from "../auth/AuthContext";
import { fetchUserProfile } from "../profile/profile.api";
import { useBookingDraft } from "./booking/BookingDraft";
import type { BookingTarget, ChosenLocation } from "./booking/BookingDraft";
import { createParcel, downloadReceipt, getQuote, itemTypeLabel } from "./parcels.api";
import type { DeliveryPayer, ParcelAddress, ParcelCreated, ParcelItemType, ParcelSize, Quote } from "./parcels.api";
import { ParcelDetailsStep } from "./booking/ParcelDetailsStep";
import type { DetailsErrors } from "./booking/ParcelDetailsStep";
import {
  normalizePhone,
  validateInstructions,
  validateItemType,
  validatePhone,
  validateReceiverName,
  validateWeight,
} from "./booking/bookingValidation";

const STEPS = ["Addresses", "Parcel details", "Review & confirm"];
const emptyAddress: ParcelAddress = { line1: "", city: "", district: "" };

const labelClass = "mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/60";

function AddressRow({
  target,
  chosen,
  youName,
  onClick,
}: {
  target: BookingTarget;
  chosen: ChosenLocation | null;
  youName?: string; // set when this row is the customer's own side of the booking
  onClick: () => void;
}) {
  const Icon = target === "pickup" ? ArrowUp : ArrowDown;
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex w-full items-center gap-4 rounded-2xl px-4 py-4 text-left transition ${
        target === "pickup" ? "bg-mist hover:bg-mist/70" : "bg-coral/10 hover:bg-coral/20"
      }`}
    >
      <span
        className={`grid h-10 w-10 shrink-0 place-items-center rounded-full text-white ${
          target === "pickup" ? "bg-ink" : "bg-coral"
        }`}
      >
        <Icon className="h-5 w-5" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-bold">
          {target === "pickup" ? "Pick up" : "Drop"}
          {youName && <span className="ml-2 text-xs font-normal text-ink/50">You, {youName}</span>}
        </span>
        {chosen ? (
          <span className="block text-sm text-ink/70">
            {chosen.address.line1}, {chosen.address.city} ({chosen.address.district})
          </span>
        ) : (
          <span className="block text-sm font-semibold text-coral">Set location</span>
        )}
      </span>
      <ChevronRight className="h-5 w-5 shrink-0 text-ink/30" />
    </button>
  );
}

export function BookParcelPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const draft = useBookingDraft();
  const [searchParams] = useSearchParams();
  const modeParam = searchParams.get("mode");
  const mode = draft.mode ?? (modeParam === "send" || modeParam === "receive" ? modeParam : null);
  const pickup = draft.pickup?.address ?? emptyAddress;
  const dropoff = draft.dropoff?.address ?? emptyAddress;
  const [step, setStep] = useState(1);
  const [receiverName, setReceiverName] = useState("");
  const [receiverPhone, setReceiverPhone] = useState("");
  const [size, setSize] = useState<ParcelSize>("M");
  const [weight, setWeight] = useState("1");
  const [instructions, setInstructions] = useState("");
  const [itemType, setItemType] = useState<ParcelItemType | "">("");
  const [youPay, setYouPay] = useState(true);
  const [touched, setTouched] = useState<Partial<Record<keyof DetailsErrors, boolean>>>({});
  const [downloading, setDownloading] = useState(false);
  const [quote, setQuote] = useState<Quote | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [created, setCreated] = useState<ParcelCreated | null>(null);

  // The mode arrives in the address (?mode=send); keep it for the other booking pages.
  useEffect(() => {
    if (modeParam === "send" || modeParam === "receive") draft.setMode(modeParam);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [modeParam]);

  // When the customer is receiving, they are the receiver: fill in their own details.
  useEffect(() => {
    if (mode !== "receive") return;
    setReceiverName((current) => current || user?.name || "");
    fetchUserProfile()
      .then((profile) => setReceiverPhone((current) => current || profile.phoneNumber || ""))
      .catch(() => undefined);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode]);

  const setLocation = (target: BookingTarget) => {
    draft.setTarget(target);
    navigate("/parcels/new/location");
  };

  // Every rule is checked on each render; an error only shows once the customer has left that field
  // or pressed Next.
  const allErrors: DetailsErrors = {
    receiverName: validateReceiverName(receiverName),
    receiverPhone: validatePhone(receiverPhone),
    itemType: validateItemType(itemType),
    weight: validateWeight(weight),
    instructions: validateInstructions(instructions),
  };
  const shownErrors: DetailsErrors = {};
  (Object.keys(allErrors) as (keyof DetailsErrors)[]).forEach((field) => {
    if (touched[field] && allErrors[field]) shownErrors[field] = allErrors[field];
  });
  // "You" pay is the sender when sending and the receiver when receiving.
  const deliveryPaidBy: DeliveryPayer = youPay === (mode === "send") ? "SENDER" : "RECEIVER";

  const addressOk = (a: ParcelAddress) => a.line1.trim() !== "" && a.city.trim() !== "" && a.district !== "";

  const next = async () => {
    setError("");
    if (step === 1) {
      if (!addressOk(pickup) || !addressOk(dropoff)) {
        setError("Please set both the pick up and drop locations.");
        return;
      }
      setStep(2);
      return;
    }

    // step 2: check every field, then ask for the price
    const weightKg = Number(weight);
    if (Object.values(allErrors).some(Boolean)) {
      setTouched({ receiverName: true, receiverPhone: true, itemType: true, weight: true, instructions: true });
      setError("Please fix the highlighted fields.");
      return;
    }
    setLoading(true);
    try {
      setQuote(await getQuote(pickup.district, dropoff.district, size, weightKg));
      setStep(3);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not get a price. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  const confirm = async () => {
    setError("");
    setLoading(true);
    try {
      const result = await createParcel({
        pickup,
        dropoff,
        receiverName: receiverName.trim(),
        receiverPhone: normalizePhone(receiverPhone),
        size,
        weightKg: Number(weight),
        deliveryInstructions: instructions.trim() || undefined,
        itemType: itemType || undefined,
        deliveryPaidBy,
      });
      setCreated(result);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not book the parcel. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  const receipt = async () => {
    if (!created) return;
    setError("");
    setDownloading(true);
    try {
      await downloadReceipt(created.id, created.trackingNumber);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not download the receipt. Please try again."));
    } finally {
      setDownloading(false);
    }
  };

  if (!mode) return <Navigate to="/parcels/start" replace />;

  if (created) {
    const rows: [string, string][] = [
      ["From", `${pickup.line1}, ${pickup.city} (${pickup.district})`],
      ["To", `${dropoff.line1}, ${dropoff.city} (${dropoff.district})`],
      ["Receiver", `${receiverName.trim()}, ${normalizePhone(receiverPhone)}`],
      ["Item", `${itemTypeLabel(itemType)}, size ${size}, ${weight} kg`],
      ["Delivery paid by", deliveryPaidBy === "SENDER" ? "Sender" : "Receiver"],
    ];
    return (
      <div className="mx-auto max-w-xl overflow-hidden rounded-2xl border border-ink/5 bg-white shadow-sm">
        <div className="bg-mint/50 px-6 py-8 text-center">
          <div className="mx-auto mb-3 grid h-14 w-14 place-items-center rounded-2xl bg-white text-2xl text-ink shadow-sm">✓</div>
          <h1 className="font-display text-3xl font-bold">Parcel booked</h1>
          <p className="mt-2 text-sm text-ink/60">Your tracking number</p>
          <p className="mt-1 text-2xl font-bold tracking-wide">{created.trackingNumber}</p>
        </div>

        <div className="space-y-5 p-6">
          <dl className="divide-y divide-ink/10 text-sm">
            {rows.map(([label, value]) => (
              <div key={label} className="flex justify-between gap-4 py-2.5">
                <dt className="shrink-0 text-xs font-semibold uppercase tracking-wide text-ink/60">{label}</dt>
                <dd className="text-right">{value}</dd>
              </div>
            ))}
          </dl>

          <div className="flex items-center justify-between rounded-xl bg-mist p-4">
            <span className="text-xs font-semibold uppercase tracking-wide text-ink/60">Delivery fee</span>
            <span className="font-display text-2xl font-bold">Rs. {Number(created.fee).toFixed(2)}</span>
          </div>

          {error && <div className="rounded-lg border border-coral/30 bg-coral/10 p-3 text-sm text-ink">{error}</div>}

          <div className="flex flex-col gap-3 sm:flex-row">
            <Link
              to={`/parcels/${created.trackingNumber}`}
              className="flex-1 rounded-xl bg-coral px-5 py-2.5 text-center text-sm font-semibold text-white"
            >
              Track this parcel
            </Link>
            <button
              type="button"
              disabled={downloading}
              onClick={receipt}
              className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-ink/15 px-5 py-2.5 text-sm font-semibold hover:bg-mist disabled:opacity-60"
            >
              <Download className="h-4 w-4" />
              {downloading ? "Downloading..." : "Download receipt"}
            </button>
          </div>
          <Link to="/parcels" className="block text-center text-sm font-semibold text-ink/70 underline">
            My parcels
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="font-display text-3xl font-bold">Book a parcel</h1>

      <ol className="my-6 flex gap-2">
        {STEPS.map((label, index) => (
          <li
            key={label}
            className={`flex-1 rounded-xl px-3 py-2 text-center text-xs font-semibold ${
              step === index + 1 ? "bg-ink text-white" : step > index + 1 ? "bg-mint text-ink" : "bg-white text-ink/50"
            }`}
          >
            {index + 1}. {label}
          </li>
        ))}
      </ol>

      <div className="space-y-6 rounded-2xl border border-ink/5 bg-white p-6 shadow-sm">
        {error && <div className="rounded-lg border border-coral/30 bg-coral/10 p-3 text-sm text-ink">{error}</div>}

        {step === 1 && (
          <div className="space-y-4">
            <h2 className="font-display text-2xl font-bold">Where to pick up and drop your parcel?</h2>
            <AddressRow
              target="pickup"
              chosen={draft.pickup}
              youName={mode === "send" ? user?.name : undefined}
              onClick={() => setLocation("pickup")}
            />
            <AddressRow
              target="drop"
              chosen={draft.dropoff}
              youName={mode === "receive" ? user?.name : undefined}
              onClick={() => setLocation("drop")}
            />
          </div>
        )}

        {step === 2 && (
          <ParcelDetailsStep
            mode={mode}
            userName={user?.name}
            values={{ receiverName, receiverPhone, itemType, size, weight, instructions, youPay }}
            errors={shownErrors}
            onChange={(patch) => {
              if (patch.receiverName !== undefined) setReceiverName(patch.receiverName);
              if (patch.receiverPhone !== undefined) setReceiverPhone(patch.receiverPhone);
              if (patch.itemType !== undefined) setItemType(patch.itemType);
              if (patch.size !== undefined) setSize(patch.size);
              if (patch.weight !== undefined) setWeight(patch.weight);
              if (patch.instructions !== undefined) setInstructions(patch.instructions);
              if (patch.youPay !== undefined) setYouPay(patch.youPay);
            }}
            onBlur={(field) => setTouched((t) => ({ ...t, [field]: true }))}
          />
        )}

        {step === 3 && quote && (
          <div className="space-y-4">
            <h2 className="font-display text-xl font-bold">Review your booking</h2>
            <dl className="grid gap-3 text-sm sm:grid-cols-2">
              <div>
                <dt className={labelClass}>From</dt>
                <dd>
                  {pickup.line1}, {pickup.city} ({pickup.district})
                </dd>
              </div>
              <div>
                <dt className={labelClass}>To</dt>
                <dd>
                  {dropoff.line1}, {dropoff.city} ({dropoff.district})
                </dd>
              </div>
              <div>
                <dt className={labelClass}>Receiver</dt>
                <dd>
                  {receiverName.trim()}, {normalizePhone(receiverPhone)}
                </dd>
              </div>
              <div>
                <dt className={labelClass}>Item</dt>
                <dd>
                  {itemTypeLabel(itemType)}, size {size}, {weight} kg
                </dd>
              </div>
              <div>
                <dt className={labelClass}>Delivery paid by</dt>
                <dd>{deliveryPaidBy === "SENDER" ? "Sender" : "Receiver"}</dd>
              </div>
              {instructions.trim() && (
                <div>
                  <dt className={labelClass}>Instructions</dt>
                  <dd className="break-words">{instructions.trim()}</dd>
                </div>
              )}
            </dl>
            <div className="rounded-xl bg-mist p-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-ink/60">Delivery fee</p>
              <p className="font-display text-3xl font-bold">Rs. {Number(quote.fee).toFixed(2)}</p>
              <p className="text-xs text-ink/60">
                {quote.pickupZone} to {quote.dropoffZone}
                {quote.interZone ? " (includes the inter-zone charge)" : ""}
              </p>
            </div>
          </div>
        )}

        <div className="flex justify-between gap-3">
          <button
            type="button"
            disabled={step === 1 || loading}
            onClick={() => setStep(step - 1)}
            className="rounded-xl border border-ink/15 px-5 py-2.5 text-sm font-semibold disabled:opacity-40"
          >
            Back
          </button>
          {step < 3 ? (
            <button
              type="button"
              disabled={loading}
              onClick={next}
              className="rounded-xl bg-coral px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
            >
              {loading ? "Please wait..." : "Next"}
            </button>
          ) : (
            <button
              type="button"
              disabled={loading}
              onClick={confirm}
              className="rounded-xl bg-coral px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60"
            >
              {loading ? "Booking..." : "Confirm booking"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}