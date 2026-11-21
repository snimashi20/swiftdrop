import { ChevronDown } from "lucide-react";
import type { ReactNode } from "react";
import { ITEM_TYPES } from "../parcels.api";
import type { ParcelItemType, ParcelSize } from "../parcels.api";
import { MAX_INSTRUCTIONS } from "./bookingValidation";
import type { BookingMode } from "./BookingDraft";

const inputBase = "w-full rounded-xl border bg-white px-3 py-2.5 text-sm outline-none focus:border-coral";
const labelClass = "mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/60";

export interface DetailsValues {
  receiverName: string;
  receiverPhone: string;
  itemType: ParcelItemType | "";
  size: ParcelSize;
  weight: string;
  instructions: string;
  youPay: boolean;
}

export type DetailsErrors = Partial<Record<"receiverName" | "receiverPhone" | "itemType" | "weight" | "instructions", string>>;

function Field({ label, htmlFor, error, children }: { label: string; htmlFor: string; error?: string; children: ReactNode }) {
  return (
    <div>
      <label htmlFor={htmlFor} className={labelClass}>
        {label}
      </label>
      {children}
      {error && (
        <p id={`${htmlFor}-error`} role="alert" className="mt-1 text-xs text-coral">
          {error}
        </p>
      )}
    </div>
  );
}

const inputClass = (error?: string) => `${inputBase} ${error ? "border-coral" : "border-ink/15"}`;

function PayCard({ selected, title, subtitle, onSelect }: { selected: boolean; title: string; subtitle: string; onSelect: () => void }) {
  return (
    <label
      className={`flex cursor-pointer items-center gap-3 rounded-xl border-2 px-4 py-3 transition ${
        selected ? "border-ink bg-mist" : "border-ink/10 bg-white hover:border-ink/30"
      }`}
    >
      <input type="radio" name="deliveryPayer" className="sr-only" checked={selected} onChange={onSelect} />
      <span className={`grid h-5 w-5 shrink-0 place-items-center rounded-full border-2 ${selected ? "border-coral" : "border-ink/30"}`}>
        {selected && <span className="h-2.5 w-2.5 rounded-full bg-coral" />}
      </span>
      <span className="min-w-0">
        <span className="block truncate text-sm font-semibold">{title}</span>
        <span className="block truncate text-xs text-ink/55">{subtitle}</span>
      </span>
    </label>
  );
}

// Second booking step: receiver, parcel, who pays, instructions. Errors are shown under each field.
export function ParcelDetailsStep({
  mode,
  userName,
  values,
  errors,
  onChange,
  onBlur,
}: {
  mode: BookingMode;
  userName?: string;
  values: DetailsValues;
  errors: DetailsErrors;
  onChange: (patch: Partial<DetailsValues>) => void;
  onBlur: (field: keyof DetailsErrors) => void;
}) {
  const sending = mode === "send";
  const you = `${userName || "You"} (You)`;
  const other = sending ? values.receiverName.trim() || "Receiver" : "Sender";
  const otherNote = sending ? values.receiverPhone.trim() || "The person receiving the parcel" : "The person sending the parcel";

  return (
    <div className="space-y-6">
      <section className="space-y-3">
        <h2 className="font-display text-xl font-bold">Receiver</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Receiver name" htmlFor="receiverName" error={errors.receiverName}>
            <input
              id="receiverName"
              className={inputClass(errors.receiverName)}
              value={values.receiverName}
              maxLength={100}
              autoComplete="off"
              aria-invalid={Boolean(errors.receiverName)}
              aria-describedby={errors.receiverName ? "receiverName-error" : undefined}
              onChange={(e) => onChange({ receiverName: e.target.value })}
              onBlur={() => onBlur("receiverName")}
            />
          </Field>
          <Field label="Receiver phone" htmlFor="receiverPhone" error={errors.receiverPhone}>
            <input
              id="receiverPhone"
              className={inputClass(errors.receiverPhone)}
              value={values.receiverPhone}
              maxLength={16}
              inputMode="tel"
              placeholder="0771234567"
              autoComplete="off"
              aria-invalid={Boolean(errors.receiverPhone)}
              aria-describedby={errors.receiverPhone ? "receiverPhone-error" : undefined}
              onChange={(e) => onChange({ receiverPhone: e.target.value })}
              onBlur={() => onBlur("receiverPhone")}
            />
          </Field>
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="font-display text-xl font-bold">Parcel</h2>
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label="Item type" htmlFor="itemType" error={errors.itemType}>
            <div className="relative">
              <select
                id="itemType"
                className={`${inputClass(errors.itemType)} appearance-none pr-9 ${values.itemType ? "" : "text-ink/50"}`}
                value={values.itemType}
                aria-invalid={Boolean(errors.itemType)}
                aria-describedby={errors.itemType ? "itemType-error" : undefined}
                onChange={(e) => onChange({ itemType: e.target.value as ParcelItemType | "" })}
                onBlur={() => onBlur("itemType")}
              >
                <option value="" disabled>
                  Select item type
                </option>
                {ITEM_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
              <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink/50" />
            </div>
          </Field>
          <Field label="Size" htmlFor="size">
            <div className="relative">
              <select
                id="size"
                className={`${inputClass()} appearance-none pr-9`}
                value={values.size}
                onChange={(e) => onChange({ size: e.target.value as ParcelSize })}
              >
                <option value="S">Small</option>
                <option value="M">Medium</option>
                <option value="L">Large</option>
              </select>
              <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink/50" />
            </div>
          </Field>
          <Field label="Weight (kg)" htmlFor="weight" error={errors.weight}>
            <input
              id="weight"
              className={inputClass(errors.weight)}
              value={values.weight}
              inputMode="decimal"
              autoComplete="off"
              aria-invalid={Boolean(errors.weight)}
              aria-describedby={errors.weight ? "weight-error" : undefined}
              onChange={(e) => onChange({ weight: e.target.value })}
              onBlur={() => onBlur("weight")}
            />
          </Field>
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="font-display text-xl font-bold">Who pays for the delivery?</h2>
        <div className="grid gap-3 sm:grid-cols-2" role="radiogroup" aria-label="Who pays for the delivery">
          <PayCard selected={values.youPay} title={you} subtitle={sending ? "Sender" : "Receiver"} onSelect={() => onChange({ youPay: true })} />
          <PayCard selected={!values.youPay} title={other} subtitle={otherNote} onSelect={() => onChange({ youPay: false })} />
        </div>
      </section>

      <section>
        <Field label="Delivery instructions (optional)" htmlFor="instructions" error={errors.instructions}>
          <textarea
            id="instructions"
            className={inputClass(errors.instructions)}
            rows={3}
            value={values.instructions}
            onChange={(e) => onChange({ instructions: e.target.value })}
            onBlur={() => onBlur("instructions")}
          />
        </Field>
        <p className="mt-1 text-right text-xs text-ink/50">
          {values.instructions.length}/{MAX_INSTRUCTIONS}
        </p>
      </section>
    </div>
  );
}
