import { zodResolver } from "@hookform/resolvers/zod";
import { Building2, CheckCircle2, MapPin, ShieldCheck } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { FormField } from "../../../components/FormField";
import { getApiErrorMessage } from "../../../lib/api";
import { fetchMyMerchant, updateMyMerchantProfile } from "../api";
import type { Merchant, UpdateMerchantProfileRequest } from "../types";

const businessTypes = ["SOLE_PROPRIETORSHIP", "PARTNERSHIP", "PRIVATE_LIMITED_COMPANY", "OTHER"] as const;
const schema = z.object({
  businessName: z.string().trim().min(2, "Enter your registered business name").max(150),
  businessType: z.enum(businessTypes, { errorMap: () => ({ message: "Select a business structure" }) }),
  registrationNumber: z.string().trim().min(1, "Enter your business registration number").max(100),
  businessAddress: z.string().trim().min(5, "Enter a business address").max(300),
  businessDescription: z.string().trim().min(10, "Describe your products or services").max(500),
  contactName: z.string().trim().min(2, "Enter a contact name").max(100),
  contactPhone: z.string().regex(/^(?:0|\+94)7\d{8}$/, "Use 07XXXXXXXX or +947XXXXXXXX"),
  verificationDeclarationAccepted: z.boolean().refine((accepted) => accepted, "Confirm that the information is accurate"),
});

type FormValues = z.infer<typeof schema>;
const emptyValues: FormValues = {
  businessName: "",
  businessType: "OTHER",
  registrationNumber: "",
  businessAddress: "",
  businessDescription: "",
  contactName: "",
  contactPhone: "",
  verificationDeclarationAccepted: false,
};

const businessTypeLabels: Record<(typeof businessTypes)[number], string> = {
  SOLE_PROPRIETORSHIP: "Sole proprietorship",
  PARTNERSHIP: "Partnership",
  PRIVATE_LIMITED_COMPANY: "Private limited company",
  OTHER: "Other",
};

export function MerchantProfilePage() {
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [saveError, setSaveError] = useState("");
  const [savedMessage, setSavedMessage] = useState("");
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: emptyValues,
  });

  useEffect(() => {
    let active = true;
    fetchMyMerchant()
      .then((data) => {
        if (!active) return;
        setMerchant(data);
        reset({
          businessName: data.businessName ?? "",
          businessType: data.businessType ?? "OTHER",
          registrationNumber: data.registrationNumber ?? "",
          businessAddress: data.businessAddress ?? "",
          businessDescription: data.businessDescription ?? "",
          contactName: data.contactName ?? "",
          contactPhone: data.contactPhone ?? "",
          verificationDeclarationAccepted: data.verificationDeclarationAccepted === true,
        });
      })
      .catch((error) => {
        if (active) setLoadError(getApiErrorMessage(error, "We could not load your merchant profile."));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [reset]);

  const submit = async (values: FormValues) => {
    setSaveError("");
    setSavedMessage("");
    try {
      const updated = await updateMyMerchantProfile(values satisfies UpdateMerchantProfileRequest);
      setMerchant(updated);
      if (updated.status === "APPROVED") {
        setSavedMessage("No verification details changed. Your approved merchant status and API access remain active.");
      } else if (updated.status === "PENDING") {
        setSavedMessage("Your profile is pending admin review. Merchant API access is paused until approval, but you can continue signing in.");
      } else {
        setSavedMessage("Your profile remains rejected. Update your business details and save them to request another review.");
      }
    } catch (error) {
      setSaveError(getApiErrorMessage(error, "We could not update your merchant profile."));
    }
  };

  if (loading) return <div className="mx-auto max-w-4xl p-8 text-sm text-ink/60">Loading your business profile…</div>;
  if (loadError) return <main className="mx-auto max-w-4xl p-6"><div role="alert" className="rounded-2xl border border-coral/20 bg-white p-5 text-sm text-coral">{loadError}</div></main>;

  return (
    <main className="mx-auto max-w-5xl p-4 pb-12 pt-24 sm:p-8 sm:pt-24">
      <header className="mb-7">
        <p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">Merchant workspace</p>
        <div className="mt-2 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="font-display text-3xl font-bold text-ink sm:text-4xl">Business profile</h1>
            <p className="mt-2 max-w-2xl text-sm text-ink/60">Keep your business and primary contact details up to date for SwiftDrop admin review.</p>
          </div>
          <span className={`rounded-full px-3 py-1.5 text-xs font-bold ${merchant?.status === "APPROVED" ? "bg-mint text-ink" : merchant?.status === "REJECTED" ? "bg-coral/10 text-coral" : "bg-mist text-ink"}`}>
            {merchant?.status ?? "PROFILE"}
          </span>
        </div>
      </header>

      {savedMessage && (
        <div role="status" className="mb-5 flex items-start gap-3 rounded-2xl border border-mint bg-mint/40 p-4 text-sm text-ink">
          <CheckCircle2 size={19} className="mt-0.5 shrink-0" />
          <p>{savedMessage}</p>
        </div>
      )}
      {merchant?.rejectionReason && (
        <div className="mb-5 rounded-2xl border border-coral/20 bg-coral/5 p-4 text-sm text-ink">
          <p className="font-semibold text-coral">Admin feedback on your previous application</p>
          <p className="mt-1">{merchant.rejectionReason}</p>
        </div>
      )}

      <form noValidate onSubmit={handleSubmit(submit)} className="space-y-5">
        <section className="rounded-2xl border border-ink/10 bg-white p-5 shadow-sm sm:p-6">
          <h2 className="mb-4 flex items-center gap-2 font-display text-lg font-bold text-ink"><Building2 size={19} className="text-coral" /> Business identity</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label="Registered business name *" placeholder="Ceylon Craft Corner" error={errors.businessName?.message} {...register("businessName")} />
            <div>
              <label htmlFor="merchant-business-type" className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-ink/70">Business structure *</label>
              <select id="merchant-business-type" {...register("businessType")} className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm text-ink outline-none focus:border-coral focus:ring-2 focus:ring-coral/20">
                {businessTypes.map((type) => <option key={type} value={type}>{businessTypeLabels[type]}</option>)}
              </select>
              {errors.businessType && <p className="mt-1 text-xs text-coral">{errors.businessType.message}</p>}
            </div>
            <FormField label="Business registration number *" placeholder="Demo BR number" error={errors.registrationNumber?.message} {...register("registrationNumber")} />
            <FormField label="Business address *" placeholder="Street, city, district, Sri Lanka" error={errors.businessAddress?.message} {...register("businessAddress")} />
            <div className="sm:col-span-2">
              <label htmlFor="merchant-business-description" className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-ink/70">Products or services *</label>
              <textarea id="merchant-business-description" rows={3} maxLength={500} {...register("businessDescription")} placeholder="Describe what your business sells or provides." className="w-full resize-y rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm text-ink outline-none placeholder:text-ink/35 focus:border-coral focus:ring-2 focus:ring-coral/20" />
              {errors.businessDescription && <p className="mt-1 text-xs text-coral">{errors.businessDescription.message}</p>}
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-ink/10 bg-white p-5 shadow-sm sm:p-6">
          <h2 className="mb-4 flex items-center gap-2 font-display text-lg font-bold text-ink"><MapPin size={19} className="text-coral" /> Primary contact</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-ink/70" htmlFor="merchant-contact-email">Account email</label>
              <input id="merchant-contact-email" value={merchant?.contactEmail ?? ""} readOnly className="w-full rounded-xl border border-ink/10 bg-mist px-3.5 py-3 text-sm text-ink/60" />
            </div>
            <FormField label="Contact person *" placeholder="Business owner" error={errors.contactName?.message} {...register("contactName")} />
            <FormField label="Sri Lankan mobile number *" placeholder="07XXXXXXXX" error={errors.contactPhone?.message} {...register("contactPhone")} />
          </div>
        </section>

        <section className="rounded-2xl border border-coral/20 bg-mist p-5 sm:p-6">
          <h2 className="mb-3 flex items-center gap-2 font-display text-lg font-bold text-ink"><ShieldCheck size={19} className="text-coral" /> Submit for review</h2>
          <p className="mb-4 text-sm text-ink/70">Changed verification details go back to the admin approval queue. While review is pending, merchant API access is paused, but your account stays enabled so you can sign in and manage your profile.</p>
          <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-ink/10 bg-white p-4 text-sm text-ink/75">
            <input type="checkbox" {...register("verificationDeclarationAccepted")} className="mt-0.5 h-4 w-4 accent-coral" />
            <span>I confirm that I am authorized to represent this business and that the information submitted is accurate.</span>
          </label>
          {errors.verificationDeclarationAccepted && <p className="mt-1.5 text-xs text-coral">{errors.verificationDeclarationAccepted.message}</p>}
        </section>

        {saveError && <p role="alert" className="rounded-xl border border-coral/20 bg-coral/10 px-4 py-3 text-sm text-coral">{saveError}</p>}
        <button type="submit" disabled={isSubmitting} className="w-full rounded-xl bg-ink px-5 py-3.5 font-semibold text-white transition hover:bg-ink/90 disabled:cursor-not-allowed disabled:opacity-60">
          {isSubmitting ? "Saving business profile..." : "Save business profile"}
        </button>
      </form>
    </main>
  );
}
