import { zodResolver } from "@hookform/resolvers/zod";
import {
  ArrowRight,
  CheckCircle2,
  CreditCard,
  ShieldCheck,
  Truck,
  Upload,
  X,
} from "lucide-react";
import React, { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { z } from "zod";
import { FormField } from "../../../components/FormField";
import { getApiErrorMessage } from "../../../lib/api";
import { getPublicCourierZones, registerCourier } from "../api";
import type { VehicleType, ZoneOption } from "../types";

const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/jpg"];

const isAtLeast18 = (dob: string) => {
  const birth = new Date(dob);
  if (Number.isNaN(birth.getTime())) return false;
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const m = today.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
  return age >= 18;
};

const isFutureDate = (dateStr: string) => {
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return d > today;
};

const schema = z.object({
  fullName: z.string().trim().min(2, "Enter your full name as on NIC").max(100),
  email: z.string().trim().email("Enter a valid email address"),
  password: z.string().min(8, "Use at least 8 characters"),
  phoneNumber: z.string().regex(/^(?:0|\+94)7\d{8}$/, "Use 07XXXXXXXX or +947XXXXXXXX"),
  nicNumber: z
    .string()
    .trim()
    .regex(/^(?:\d{9}[VvXx]|\d{12})$/, "Enter a valid NIC (e.g., 991234567V or 200112345678)"),
  dateOfBirth: z
    .string()
    .min(1, "Date of birth is required")
    .refine(isAtLeast18, "You must be at least 18 years old to register as a courier"),
  residentialAddress: z.string().trim().min(5, "Enter your full residential address").max(300),
  vehicleType: z.enum(["BICYCLE", "MOTORBIKE", "THREE_WHEELER", "CAR", "VAN"] as const),
  vehiclePlateNumber: z.string().trim().min(3, "Enter your vehicle registration plate (e.g., WP BEX-1234)").max(30),
  drivingLicenseNumber: z.string().trim().min(4, "Enter your driving license number").max(50),
  drivingLicenseExpiry: z
    .string()
    .min(1, "Driving license expiry date is required")
    .refine(isFutureDate, "Driving license must be valid and not expired"),
  zoneId: z.coerce.number().int().positive("Select an operating delivery zone"),
  emergencyContactName: z.string().trim().min(2, "Enter an emergency contact name").max(100),
  emergencyContactPhone: z.string().regex(/^(?:0|\+94)7\d{8}$/, "Use 07XXXXXXXX or +947XXXXXXXX"),
  declarationAccepted: z.literal(true, {
    errorMap: () => ({ message: "You must confirm the background and safety declaration" }),
  }),
});

type FormValues = z.infer<typeof schema>;

const VEHICLE_OPTIONS: { value: VehicleType; label: string }[] = [
  { value: "MOTORBIKE", label: "Motorbike" },
  { value: "THREE_WHEELER", label: "Three-Wheeler (Tuk-Tuk)" },
  { value: "CAR", label: "Car" },
  { value: "VAN", label: "Delivery Van" },
  { value: "BICYCLE", label: "Bicycle" },
];

export function CourierRegisterPage() {
  const [zones, setZones] = useState<ZoneOption[]>([]);
  const [zonesLoading, setZonesLoading] = useState(true);
  const [zonesError, setZonesError] = useState("");

  const [nicFront, setNicFront] = useState<File | null>(null);
  const [nicFrontPreview, setNicFrontPreview] = useState<string | null>(null);
  const [nicFrontError, setNicFrontError] = useState("");

  const [nicBack, setNicBack] = useState<File | null>(null);
  const [nicBackPreview, setNicBackPreview] = useState<string | null>(null);
  const [nicBackError, setNicBackError] = useState("");

  const [serverError, setServerError] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting, isValid },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: "onChange",
    defaultValues: {
      vehicleType: "MOTORBIKE",
    },
  });

  useEffect(() => {
    let active = true;
    setZonesLoading(true);
    getPublicCourierZones()
      .then((data) => {
        if (active) setZones(data);
      })
      .catch(() => {
        if (active) setZonesError("Could not load country-wide delivery zones. Please refresh.");
      })
      .finally(() => {
        if (active) setZonesLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  // Clean up preview blob URLs on unmount
  useEffect(() => {
    return () => {
      if (nicFrontPreview) URL.revokeObjectURL(nicFrontPreview);
      if (nicBackPreview) URL.revokeObjectURL(nicBackPreview);
    };
  }, [nicFrontPreview, nicBackPreview]);

  const handleNicFile = (
    side: "front" | "back",
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const setErr = side === "front" ? setNicFrontError : setNicBackError;
    const setFile = side === "front" ? setNicFront : setNicBack;
    const setPreview = side === "front" ? setNicFrontPreview : setNicBackPreview;
    const oldPreview = side === "front" ? nicFrontPreview : nicBackPreview;

    if (!ALLOWED_TYPES.includes(file.type.toLowerCase())) {
      setErr("Only JPEG and PNG images are allowed.");
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      setErr("Image must be smaller than 5MB.");
      return;
    }

    if (oldPreview) URL.revokeObjectURL(oldPreview);
    setFile(file);
    setPreview(URL.createObjectURL(file));
    setErr("");
  };

  const removeNicFile = (side: "front" | "back") => {
    if (side === "front") {
      if (nicFrontPreview) URL.revokeObjectURL(nicFrontPreview);
      setNicFront(null);
      setNicFrontPreview(null);
      setNicFrontError("NIC front side photo is required.");
    } else {
      if (nicBackPreview) URL.revokeObjectURL(nicBackPreview);
      setNicBack(null);
      setNicBackPreview(null);
      setNicBackError("NIC back side photo is required.");
    }
  };

  const onSubmit = async (values: FormValues) => {
    setServerError("");
    let missingFile = false;

    if (!nicFront) {
      setNicFrontError("Please upload the front side of your NIC.");
      missingFile = true;
    }
    if (!nicBack) {
      setNicBackError("Please upload the back side of your NIC.");
      missingFile = true;
    }
    if (missingFile || !nicFront || !nicBack) return;

    try {
      const { declarationAccepted: _ignored, ...payload } = values;
      await registerCourier(payload, nicFront, nicBack);
      setSubmitted(true);
    } catch (err) {
      setServerError(getApiErrorMessage(err, "We could not submit your courier application."));
    }
  };

  const canSubmit = isValid && Boolean(nicFront) && Boolean(nicBack) && !isSubmitting;

  return (
    <main className="min-h-screen bg-ink px-4 py-8 font-sans text-ink sm:px-6 sm:py-12">
      <div className="mx-auto max-w-4xl rounded-3xl bg-mist p-6 shadow-xl sm:p-10">
        <Link to="/login" className="font-display text-2xl font-bold">
          swift<span className="text-coral">drop</span>
        </Link>

        {submitted ? (
          <section className="py-12 text-center">
            <div className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-mint">
              <CheckCircle2 className="text-ink" size={28} />
            </div>
            <p className="mt-6 text-xs font-bold uppercase tracking-[0.2em] text-coral">
              Application Submitted
            </p>
            <h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">
              Your courier verification is under review.
            </h1>
            <p className="mx-auto mt-3 max-w-lg text-sm text-ink/65">
              Our admin team will verify your NIC documents, driving license, and assigned delivery zone.
              Once approved, you will be able to sign in and start accepting deliveries.
            </p>
            <Link
              to="/login"
              className="mt-7 inline-flex items-center gap-2 rounded-xl bg-ink px-6 py-3 font-semibold text-white transition hover:bg-coral"
            >
              Go to sign in <ArrowRight size={17} />
            </Link>
          </section>
        ) : (
          <>
            <div className="mb-8 mt-8">
              <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">
                Courier Driver Onboarding
              </p>
              <h1 className="font-display text-3xl font-bold sm:text-4xl">
                Apply to deliver with SwiftDrop.
              </h1>
              <p className="mt-2 max-w-2xl text-sm text-ink/60">
                Complete all required identity, vehicle, and operating zone details and upload clear
                photos of both sides of your National Identity Card (NIC). Your account will be activated
                after admin verification.
              </p>
            </div>

            <form noValidate onSubmit={handleSubmit(onSubmit)} className="space-y-8">
              {/* Section 1: Personal Identity */}
              <section className="rounded-2xl border border-ink/10 bg-white p-5 sm:p-6">
                <h2 className="mb-4 flex items-center gap-2 font-display text-lg font-bold">
                  <ShieldCheck size={19} className="text-coral" />
                  1. Personal & Account Details
                </h2>
                <div className="grid gap-4 sm:grid-cols-2">
                  <FormField
                    label="Full Name (as on NIC) *"
                    placeholder="Kamal Perera"
                    error={errors.fullName?.message}
                    {...register("fullName")}
                  />
                  <FormField
                    label="Email Address *"
                    type="email"
                    placeholder="rider@example.com"
                    error={errors.email?.message}
                    {...register("email")}
                  />
                  <FormField
                    label="Password *"
                    type="password"
                    placeholder="At least 8 characters"
                    error={errors.password?.message}
                    {...register("password")}
                  />
                  <FormField
                    label="Mobile Number *"
                    placeholder="07XXXXXXXX"
                    error={errors.phoneNumber?.message}
                    {...register("phoneNumber")}
                  />
                  <FormField
                    label="National Identity Card (NIC) Number *"
                    placeholder="200112345678 or 991234567V"
                    error={errors.nicNumber?.message}
                    {...register("nicNumber")}
                  />
                  <FormField
                    label="Date of Birth (Must be 18+) *"
                    type="date"
                    error={errors.dateOfBirth?.message}
                    {...register("dateOfBirth")}
                  />
                  <div className="sm:col-span-2">
                    <FormField
                      label="Residential Address *"
                      placeholder="No. 45, Galle Road, Dehiwala"
                      error={errors.residentialAddress?.message}
                      {...register("residentialAddress")}
                    />
                  </div>
                </div>
              </section>

              {/* Section 2: NIC Photo Upload (Both Sides) */}
              <section className="rounded-2xl border border-ink/10 bg-white p-5 sm:p-6">
                <h2 className="mb-1 flex items-center gap-2 font-display text-lg font-bold">
                  <CreditCard size={19} className="text-coral" />
                  2. National Identity Card (NIC) Verification Photos
                </h2>
                <p className="mb-4 text-xs text-ink/55">
                  Upload clear JPEG or PNG photos (max 5MB each) of both the front and back sides of your NIC.
                </p>

                <div className="grid gap-5 sm:grid-cols-2">
                  {/* NIC Front */}
                  <div className="rounded-xl border border-dashed border-ink/20 bg-mist/30 p-4">
                    <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-ink/75">
                      NIC Front Side *
                    </label>
                    {nicFrontPreview ? (
                      <div className="relative">
                        <img
                          src={nicFrontPreview}
                          alt="NIC Front Preview"
                          className="h-40 w-full rounded-lg border border-ink/10 object-cover"
                        />
                        <button
                          type="button"
                          onClick={() => removeNicFile("front")}
                          className="absolute right-2 top-2 inline-flex items-center gap-1 rounded-full bg-ink/80 px-2.5 py-1 text-xs font-semibold text-white hover:bg-coral"
                        >
                          <X size={13} /> Remove
                        </button>
                      </div>
                    ) : (
                      <label className="flex h-40 cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border border-ink/10 bg-white text-center transition hover:border-coral">
                        <Upload size={22} className="text-coral" />
                        <span className="text-xs font-semibold text-ink">
                          Tap to upload NIC Front
                        </span>
                        <span className="text-[11px] text-ink/45">JPEG or PNG, max 5MB</span>
                        <input
                          type="file"
                          accept="image/jpeg,image/png"
                          onChange={(e) => handleNicFile("front", e)}
                          className="hidden"
                        />
                      </label>
                    )}
                    {nicFrontError && (
                      <p className="mt-2 text-xs font-medium text-coral">{nicFrontError}</p>
                    )}
                  </div>

                  {/* NIC Back */}
                  <div className="rounded-xl border border-dashed border-ink/20 bg-mist/30 p-4">
                    <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-ink/75">
                      NIC Back Side *
                    </label>
                    {nicBackPreview ? (
                      <div className="relative">
                        <img
                          src={nicBackPreview}
                          alt="NIC Back Preview"
                          className="h-40 w-full rounded-lg border border-ink/10 object-cover"
                        />
                        <button
                          type="button"
                          onClick={() => removeNicFile("back")}
                          className="absolute right-2 top-2 inline-flex items-center gap-1 rounded-full bg-ink/80 px-2.5 py-1 text-xs font-semibold text-white hover:bg-coral"
                        >
                          <X size={13} /> Remove
                        </button>
                      </div>
                    ) : (
                      <label className="flex h-40 cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border border-ink/10 bg-white text-center transition hover:border-coral">
                        <Upload size={22} className="text-coral" />
                        <span className="text-xs font-semibold text-ink">
                          Tap to upload NIC Back
                        </span>
                        <span className="text-[11px] text-ink/45">JPEG or PNG, max 5MB</span>
                        <input
                          type="file"
                          accept="image/jpeg,image/png"
                          onChange={(e) => handleNicFile("back", e)}
                          className="hidden"
                        />
                      </label>
                    )}
                    {nicBackError && (
                      <p className="mt-2 text-xs font-medium text-coral">{nicBackError}</p>
                    )}
                  </div>
                </div>
              </section>

              {/* Section 3: Vehicle, License & Operating Zone */}
              <section className="rounded-2xl border border-ink/10 bg-white p-5 sm:p-6">
                <h2 className="mb-4 flex items-center gap-2 font-display text-lg font-bold">
                  <Truck size={19} className="text-coral" />
                  3. Vehicle, Driving License & Operating Delivery Zone
                </h2>
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-ink/70">
                      Vehicle Type *
                    </label>
                    <select
                      {...register("vehicleType")}
                      className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm font-medium text-ink outline-none focus:border-coral focus:ring-2 focus:ring-coral/20"
                    >
                      {VEHICLE_OPTIONS.map((v) => (
                        <option key={v.value} value={v.value}>
                          {v.label}
                        </option>
                      ))}
                    </select>
                    {errors.vehicleType && (
                      <p className="mt-1 text-xs text-coral">{errors.vehicleType.message}</p>
                    )}
                  </div>

                  <FormField
                    label="Vehicle Registration / Plate No. *"
                    placeholder="WP BEX-1234"
                    error={errors.vehiclePlateNumber?.message}
                    {...register("vehiclePlateNumber")}
                  />

                  <FormField
                    label="Driving License Number *"
                    placeholder="B-1234567"
                    error={errors.drivingLicenseNumber?.message}
                    {...register("drivingLicenseNumber")}
                  />

                  <FormField
                    label="Driving License Expiry Date *"
                    type="date"
                    error={errors.drivingLicenseExpiry?.message}
                    {...register("drivingLicenseExpiry")}
                  />

                  {/* Country-wide 25-District Operating Zone Dropdown */}
                  <div className="sm:col-span-2">
                    <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-ink/70">
                      Operating Delivery Zone (Country-Wide — {zones.length} Districts) *
                    </label>
                    <select
                      {...register("zoneId")}
                      disabled={zonesLoading || zones.length === 0}
                      defaultValue=""
                      className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm font-medium text-ink outline-none focus:border-coral focus:ring-2 focus:ring-coral/20 disabled:bg-mist"
                    >
                      <option value="" disabled>
                        {zonesLoading
                          ? "Loading all delivery zones..."
                          : "Select your operating district / zone"}
                      </option>
                      {zones.map((z) => (
                        <option key={z.id} value={z.id}>
                          {z.district} — {z.name}
                        </option>
                      ))}
                    </select>
                    {zonesError && <p className="mt-1 text-xs text-coral">{zonesError}</p>}
                    {errors.zoneId && (
                      <p className="mt-1 text-xs text-coral">{errors.zoneId.message}</p>
                    )}
                  </div>
                </div>
              </section>

              {/* Section 4: Emergency Contact & Declaration */}
              <section className="rounded-2xl border border-ink/10 bg-white p-5 sm:p-6">
                <h2 className="mb-4 font-display text-lg font-bold">
                  4. Emergency Contact & Safety Declaration
                </h2>
                <div className="grid gap-4 sm:grid-cols-2">
                  <FormField
                    label="Emergency Contact Name *"
                    placeholder="Nimal Perera (Parent / Spouse)"
                    error={errors.emergencyContactName?.message}
                    {...register("emergencyContactName")}
                  />
                  <FormField
                    label="Emergency Contact Phone *"
                    placeholder="07XXXXXXXX"
                    error={errors.emergencyContactPhone?.message}
                    {...register("emergencyContactPhone")}
                  />
                </div>

                <label className="mt-5 flex items-start gap-3 rounded-xl border border-ink/10 bg-mist/40 p-4 text-xs text-ink/75 cursor-pointer">
                  <input
                    type="checkbox"
                    {...register("declarationAccepted")}
                    className="mt-0.5 h-4 w-4 rounded border-ink/30 text-coral focus:ring-coral"
                  />
                  <span>
                    I declare that all information and NIC photographs submitted above are authentic,
                    I hold a valid driving license with a clean record, and I agree to
                    SwiftDrop courier verification and delivery policies.
                  </span>
                </label>
                {errors.declarationAccepted && (
                  <p className="mt-1.5 text-xs text-coral">{errors.declarationAccepted.message}</p>
                )}
              </section>

              {serverError && (
                <p
                  role="alert"
                  className="rounded-xl border border-coral/20 bg-coral/10 px-4 py-3 text-sm font-medium text-coral"
                >
                  {serverError}
                </p>
              )}

              <button
                type="submit"
                disabled={!canSubmit}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-coral px-5 py-4 font-semibold text-white transition hover:bg-coral/90 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isSubmitting ? "Uploading documents & submitting..." : "Submit Courier Application"}
                <ArrowRight size={17} />
              </button>

              {!canSubmit && !isSubmitting && (
                <p className="text-center text-xs text-ink/50">
                  Fill all required fields and upload both NIC Front &amp; Back photos to enable submission.
                </p>
              )}
            </form>

            <p className="mt-6 text-center text-sm text-ink/55">
              Already registered?{" "}
              <Link to="/login" className="font-semibold text-coral">
                Sign in
              </Link>
            </p>
          </>
        )}
      </div>
    </main>
  );
}