import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Store } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { z } from "zod";
import { FormField } from "../../../components/FormField";
import { getApiErrorMessage } from "../../../lib/api";
import { registerMerchant } from "../api";
import type { RegisterMerchantRequest } from "../types";

const schema = z.object({
  businessName: z.string().trim().min(2, "Enter your business name").max(150),
  registrationNumber: z.string().trim().min(1, "Enter your business registration number").max(100),
  contactName: z.string().trim().min(2, "Enter a contact name").max(100),
  email: z.string().trim().email("Enter a valid email address"),
  password: z.string().min(8, "Use at least 8 characters"),
  contactPhone: z.string().regex(/^(?:0|\+94)7\d{8}$/, "Use 07XXXXXXXX or +947XXXXXXXX"),
});

export function MerchantRegisterPage() {
  const [error, setError] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RegisterMerchantRequest>({ resolver: zodResolver(schema) });

  const submit = async (data: RegisterMerchantRequest) => {
    setError("");
    try {
      await registerMerchant(data);
      setSubmitted(true);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "We could not submit your merchant application."));
    }
  };

  return <main className="min-h-screen bg-ink px-4 py-8 font-sans text-ink sm:px-6 sm:py-12">
    <div className="mx-auto max-w-3xl rounded-3xl bg-mist p-6 shadow-xl sm:p-10">
      <Link to="/login" className="font-display text-2xl font-bold">swift<span className="text-coral">drop</span></Link>
      {submitted ? <section className="py-12 text-center">
        <div className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-mint"><Store className="text-ink" /></div>
        <p className="mt-6 text-xs font-bold uppercase tracking-[0.2em] text-coral">Application received</p>
        <h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">We’ll review your business.</h1>
        <p className="mx-auto mt-3 max-w-lg text-ink/60">Your merchant account is pending approval. You can sign in after registration and check your application status.</p>
        <Link to="/login" className="mt-7 inline-flex items-center gap-2 rounded-xl bg-ink px-5 py-3 font-semibold text-white">Go to sign in <ArrowRight size={17} /></Link>
      </section> : <>
        <div className="mb-8 mt-10">
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-coral">Merchant onboarding</p>
          <h1 className="font-display text-3xl font-bold sm:text-4xl">Register your business.</h1>
          <p className="mt-2 max-w-xl text-sm text-ink/55">Submit your business details to request access to the SwiftDrop merchant network.</p>
        </div>
        <form onSubmit={handleSubmit(submit)} className="grid gap-5 sm:grid-cols-2">
          <FormField label="Business name" placeholder="Northstar Goods" error={errors.businessName?.message} {...register("businessName")} />
          <FormField label="Registration number" placeholder="BR-123456" error={errors.registrationNumber?.message} {...register("registrationNumber")} />
          <FormField label="Contact name" placeholder="Alex Morgan" error={errors.contactName?.message} {...register("contactName")} />
          <FormField label="Business email" type="email" placeholder="orders@example.com" error={errors.email?.message} {...register("email")} />
          <FormField label="Password" type="password" placeholder="At least 8 characters" error={errors.password?.message} {...register("password")} />
          <FormField label="Sri Lankan mobile number" placeholder="07XXXXXXXX" error={errors.contactPhone?.message} {...register("contactPhone")} />
          {error && <p role="alert" className="rounded-xl bg-coral/10 px-4 py-3 text-sm text-coral sm:col-span-2">{error}</p>}
          <button disabled={isSubmitting} className="flex items-center justify-center gap-2 rounded-xl bg-coral px-5 py-3.5 font-semibold text-white transition hover:bg-coral/90 disabled:opacity-60 sm:col-span-2">
            {isSubmitting ? "Submitting application…" : "Submit merchant application"}<ArrowRight size={17} />
          </button>
        </form>
        <p className="mt-6 text-center text-sm text-ink/55">Already registered? <Link to="/login" className="font-semibold text-coral">Sign in</Link></p>
      </>}
    </div>
  </main>;
}
