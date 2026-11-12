import { useState } from "react";
import { Link } from "react-router-dom";
import { getApiErrorMessage } from "../../lib/api";
import { createParcel, DISTRICTS, getQuote } from "./parcels.api";
import type { ParcelAddress, ParcelCreated, ParcelSize, Quote } from "./parcels.api";

const STEPS = ["Addresses", "Parcel details", "Review & confirm"];
const emptyAddress: ParcelAddress = { line1: "", city: "", district: "Colombo" };

const inputClass =
  "w-full rounded-xl border border-ink/15 bg-white px-3 py-2.5 text-sm outline-none focus:border-coral";
const labelClass = "mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/60";

function AddressFields({
  title,
  value,
  onChange,
}: {
  title: string;
  value: ParcelAddress;
  onChange: (address: ParcelAddress) => void;
}) {
  return (
    <div className="space-y-3">
      <h2 className="font-display text-xl font-bold">{title}</h2>
      <div>
        <label className={labelClass}>Address</label>
        <input
          className={inputClass}
          value={value.line1}
          maxLength={200}
          onChange={(e) => onChange({ ...value, line1: e.target.value })}
        />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className={labelClass}>City</label>
          <input
            className={inputClass}
            value={value.city}
            maxLength={100}
            onChange={(e) => onChange({ ...value, city: e.target.value })}
          />
        </div>
        <div>
          <label className={labelClass}>District</label>
          <select
            className={inputClass}
            value={value.district}
            onChange={(e) => onChange({ ...value, district: e.target.value })}
          >
            {DISTRICTS.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
}

export function BookParcelPage() {
  const [step, setStep] = useState(1);
  const [pickup, setPickup] = useState<ParcelAddress>(emptyAddress);
  const [dropoff, setDropoff] = useState<ParcelAddress>(emptyAddress);
  const [receiverName, setReceiverName] = useState("");
  const [receiverPhone, setReceiverPhone] = useState("");
  const [size, setSize] = useState<ParcelSize>("M");
  const [weight, setWeight] = useState("1");
  const [instructions, setInstructions] = useState("");
  const [quote, setQuote] = useState<Quote | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [created, setCreated] = useState<ParcelCreated | null>(null);

  const addressOk = (a: ParcelAddress) => a.line1.trim() !== "" && a.city.trim() !== "" && a.district !== "";

  const next = async () => {
    setError("");
    if (step === 1) {
      if (!addressOk(pickup) || !addressOk(dropoff)) {
        setError("Please fill in every address field.");
        return;
      }
      setStep(2);
      return;
    }

    // step 2: check the details, then ask for the price
    const weightKg = Number(weight);
    if (!receiverName.trim() || !receiverPhone.trim()) {
      setError("Please enter the receiver's name and phone number.");
      return;
    }
    if (!(weightKg > 0) || weightKg > 9999.99) {
      setError("Weight must be more than 0 and at most 9999.99 kg.");
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
        receiverPhone: receiverPhone.trim(),
        size,
        weightKg: Number(weight),
        deliveryInstructions: instructions.trim() || undefined,
      });
      setCreated(result);
    } catch (err) {
      setError(getApiErrorMessage(err, "Could not book the parcel. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  if (created) {
    return (
      <div className="mx-auto max-w-xl rounded-2xl border border-ink/5 bg-white p-6 text-center shadow-sm">
        <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-mint text-2xl">✓</div>
        <h1 className="font-display text-3xl font-bold">Parcel booked</h1>
        <p className="mt-2 text-ink/60">Your tracking number</p>
        <p className="mt-1  text-2xl font-bold">{created.trackingNumber}</p>
        <p className="mt-2 text-sm text-ink/60">Fee: Rs. {Number(created.fee).toFixed(2)}</p>
        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Link
            to={`/parcels/${created.trackingNumber}`}
            className="rounded-xl bg-coral px-5 py-2.5 text-sm font-semibold text-white"
          >
            Track this parcel
          </Link>
          <Link to="/parcels" className="rounded-xl bg-ink px-5 py-2.5 text-sm font-semibold text-white">
            My parcels
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl">
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
          <>
            <AddressFields title="Pickup address" value={pickup} onChange={setPickup} />
            <AddressFields title="Delivery address" value={dropoff} onChange={setDropoff} />
          </>
        )}

        {step === 2 && (
          <div className="space-y-3">
            <h2 className="font-display text-xl font-bold">Parcel and receiver</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <label className={labelClass}>Receiver name</label>
                <input
                  className={inputClass}
                  value={receiverName}
                  maxLength={100}
                  onChange={(e) => setReceiverName(e.target.value)}
                />
              </div>
              <div>
                <label className={labelClass}>Receiver phone</label>
                <input
                  className={inputClass}
                  value={receiverPhone}
                  maxLength={20}
                  onChange={(e) => setReceiverPhone(e.target.value)}
                />
              </div>
              <div>
                <label className={labelClass}>Size</label>
                <select className={inputClass} value={size} onChange={(e) => setSize(e.target.value as ParcelSize)}>
                  <option value="S">Small</option>
                  <option value="M">Medium</option>
                  <option value="L">Large</option>
                </select>
              </div>
              <div>
                <label className={labelClass}>Weight (kg)</label>
                <input
                  className={inputClass}
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={weight}
                  onChange={(e) => setWeight(e.target.value)}
                />
              </div>
            </div>
            <div>
              <label className={labelClass}>Delivery instructions (optional)</label>
              <textarea
                className={inputClass}
                rows={3}
                maxLength={500}
                value={instructions}
                onChange={(e) => setInstructions(e.target.value)}
              />
            </div>
          </div>
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
                  {receiverName}, {receiverPhone}
                </dd>
              </div>
              <div>
                <dt className={labelClass}>Parcel</dt>
                <dd>
                  Size {size}, {weight} kg
                </dd>
              </div>
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