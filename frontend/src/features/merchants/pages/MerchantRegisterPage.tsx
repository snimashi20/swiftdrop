import { BrandMark } from "../../../components/BrandMark";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Building2, MapPin, ShieldCheck, Store } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { z } from "zod";
import { FormField } from "../../../components/FormField";
import { getApiErrorMessage } from "../../../lib/api";
import { registerMerchant } from "../api";
import type { RegisterMerchantRequest } from "../types";

const businessTypes = ["SOLE_PROPRIETORSHIP", "PARTNERSHIP", "PRIVATE_LIMITED_COMPANY", "OTHER"] as const;

const schema = z.object({
  businessName: z.string().trim().min(2, "Enter your registered business name").max(150),
  businessType: z.enum(businessTypes, { errorMap: () => ({ message: "Select a business type" }) }),
  registrationNumber: z.string().trim().min(1, "Enter your business registration number").max(100),
  businessAddress: z.string().trim().min(5, "Enter the registered or operating business address").max(300),
  businessDescription: z.string().trim().min(10, "Describe your products or services (at least 10 characters)").max(500),
  contactName: z.string().trim().min(2, "Enter a contact name").max(100),
  email: z.string().trim().email("Enter a valid email address"),
  password: z.string().min(8, "Use at least 8 characters"),
  contactPhone: z.string().regex(/^(?:0|\+94)7\d{8}$/, "Use 07XXXXXXXX or +947XXXXXXXX"),
  verificationDeclarationAccepted: z.literal(true, {
    errorMap: () => ({ message: "Confirm that the business information is accurate" }),
  }),
});

type FormValues = z.infer<typeof schema>;

const businessTypeLabels: Record<(typeof businessTypes)[number], string> = {
  SOLE_PROPRIETORSHIP: "Sole proprietorship",
  PARTNERSHIP: "Partnership",
  PRIVATE_LIMITED_COMPANY: "Private limited company",
  OTHER: "Other registered business",
};

export function MerchantRegisterPage() {
  const [error, setError] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    mode: "onChange",
  });

  const submit = async (data: FormValues) => {
    setError("");
    try {
      await registerMerchant(data satisfies RegisterMerchantRequest);
      setSubmitted(true);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "We could not submit your merchant application."));
    }
  };

  return (
    <main className="min-h-screen bg-ink px-4 py-8 font-sans text-ink sm:px-6 sm:py-12">
      <div className="mx-auto max-w-4xl rounded-3xl bg-mist p-6 shadow-xl sm:p-10">
        <Link to="/login" className="font-display text-2xl font-bold">
          <BrandMark />swift<span className="text-coral">drop</span>
        </Link>

        {submitted ? (
          <section className="py-12 text-center">
            <div className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-mint">
              <Store className="text-ink" />
            </div>
            <p className="mt-6 text-xs font-bold uppercase tracking-[0.2em] text-coral">Application received</p>
            <h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">We’ll review your business.</h1>
            <p className="mx-auto mt-3 max-w-lg text-sm text-ink/60">
              Your application and business details have been sent to the admin team for review. Your account will remain pending until it is approved.
            </p>
            <Link to="/login" className="mt-7 inline-flex items-center gap-2 rounded-xl bg-ink px-5 py-3 font-semibold text-white">
              Go to sign in <ArrowRight size={17} />
            </Link>
          </section>
        ) : (
          <>
            <header className="mb-8 mt-8">
              <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">Merchant onboarding</p>
              <h1 className="font-display text-3xl font-bold sm:text-4xl">Apply to join SwiftDrop.</h1>
              <p className="mt-2 max-w-2xl text-sm text-ink/60">
                Provide your registration number, business address, and a short description so our admin team can review your application. No documents need to be uploaded.
              </p>
            </header>

            <form noValidate onSubmit={handleSubmit(submit)} className="space-y-6">
              <section className="rounded-2xl border border-ink/10 bg-white p-5 sm:p-6">
                <h2 className="mb-4 flex items-center gap-2 font-display text-lg font-bold">
                  <Building2 size={19} className="text-coral" /> Business identity
                </h2>
                <div className="grid gap-4 sm:grid-cols-2">
                  <FormField label="Registered business name *" placeholder="Northstar Goods" error={errors.businessName?.message} {...register("businessName")} />
                  <div>
                    <label htmlFor="businessType" className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-ink/70">Business structure *</label>
                    <select id="businessType" {...register("businessType")} defaultValue="" className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm font-medium text-ink outline-none focus:border-coral focus:ring-2 focus:ring-coral/20">
                      <option value="" disabled>Select business structure</option>
                      {businessTypes.map((type) => <option key={type} value={type}>{businessTypeLabels[type]}</option>)}
                    </select>
                    {errors.businessType && <p className="mt-1 text-xs text-coral">{errors.businessType.message}</p>}
                  </div>
                  <FormField label="Business registration number *" placeholder="BR-123456" error={errors.registrationNumber?.message} {...register("registrationNumber")} />
                  <FormField label="Registered or operating address *" placeholder="Street, city, district" error={errors.businessAddress?.message} {...register("businessAddress")} />
                  <div className="sm:col-span-2">
                    <label htmlFor="businessDescription" className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-ink/70">Products or services *</label>
                    <textarea id="businessDescription" rows={3} maxLength={500} placeholder="Briefly describe what your business sells or provides." {...register("businessDescription")} className="w-full resize-y rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm text-ink outline-none placeholder:text-ink/35 focus:border-coral focus:ring-2 focus:ring-coral/20" />
                    {errors.businessDescription && <p className="mt-1 text-xs text-coral">{errors.businessDescription.message}</p>}
                    <p className="mt-1 text-right text-xs text-ink/45">Maximum 500 characters</p>
                  </div>
                </div>
              </section>

              <section className="rounded-2xl border border-ink/10 bg-white p-5 sm:p-6">
                <h2 className="mb-4 flex items-center gap-2 font-display text-lg font-bold">
                  <MapPin size={19} className="text-coral" /> Account and primary contact
                </h2>
                <div className="grid gap-4 sm:grid-cols-2">
                  <FormField label="Contact person *" placeholder="Alex Morgan" error={errors.contactName?.message} {...register("contactName")} />
                  <FormField label="Business email / sign-in email *" type="email" placeholder="orders@example.com" error={errors.email?.message} {...register("email")} />
                  <FormField label="Password *" type="password" placeholder="At least 8 characters" error={errors.password?.message} {...register("password")} />
                  <FormField label="Sri Lankan mobile number *" placeholder="07XXXXXXXX" error={errors.contactPhone?.message} {...register("contactPhone")} />
                </div>
              </section>

              <section className="rounded-2xl border border-ink/10 bg-white p-5 sm:p-6">
                <h2 className="mb-3 flex items-center gap-2 font-display text-lg font-bold">
                  <ShieldCheck size={19} className="text-coral" /> Confirmation
                </h2>
                <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-ink/10 bg-mist/50 p-4 text-sm text-ink/75">
                  <input type="checkbox" {...register("verificationDeclarationAccepted")} className="mt-0.5 h-4 w-4 rounded border-ink/30 text-coral focus:ring-coral" />
                  <span>I confirm that I am authorized to represent this business and that the information submitted is accurate.</span>
                </label>
                {errors.verificationDeclarationAccepted && <p className="mt-1.5 text-xs text-coral">{errors.verificationDeclarationAccepted.message}</p>}
              </section>

              {error && <p role="alert" className="rounded-xl bg-coral/10 px-4 py-3 text-sm text-coral">{error}</p>}
              <button type="submit" disabled={isSubmitting} className="flex w-full items-center justify-center gap-2 rounded-xl bg-coral px-5 py-4 font-semibold text-white transition hover:bg-coral/90 disabled:cursor-not-allowed disabled:opacity-60">
                {isSubmitting ? "Submitting application…" : "Submit business application"}<ArrowRight size={17} />
              </button>
            </form>
            <p className="mt-6 text-center text-sm text-ink/55">Already registered? <Link to="/login" className="font-semibold text-coral">Sign in</Link></p>
          </>
        )}
      </div>
    </main>
  );
}
