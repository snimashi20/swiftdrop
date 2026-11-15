import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, ShieldAlert, Sparkles, Building2, Truck, User as UserIcon } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
import { FormField } from "../../components/FormField";
import { getApiErrorMessage } from "../../lib/api";
import { useAuth } from "./AuthContext";
import { GoogleLoginButton } from "./GoogleLoginButton";
import type { SignupInput } from "./auth.types";

const schema = z.object({
  name: z.string().min(2, "Enter your full name"),
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(8, "Use at least 8 characters"),
  role: z.enum(["CUSTOMER", "MERCHANT", "COURIER"]),
});

export function SignupPage() {
  const { signup, isLoading } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState("");

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<SignupInput>({
    resolver: zodResolver(schema),
    defaultValues: { role: "CUSTOMER" },
  });

  const selectedRole = watch("role");

  const submit = async (data: SignupInput) => {
    setServerError("");
    try {
      const user = await signup(data);
      if (user.status === "PENDING_APPROVAL" || data.role === "MERCHANT" || data.role === "COURIER") {
        navigate("/waiting-approval", { replace: true });
      } else {
        navigate("/dashboard", { replace: true });
      }
    } catch (error) {
      setServerError(getApiErrorMessage(error, "We could not create your account."));
    }
  };

  return (
    <div className="min-h-screen bg-ink p-4 sm:p-8 flex items-center justify-center">
      <div className="mx-auto max-w-2xl w-full rounded-3xl bg-[#fbfcfa] p-6 sm:p-12 shadow-2xl">
        <Link to="/login" className="font-display text-2xl font-bold">
          swift<span className="text-coral">drop</span>
        </Link>

        <div className="my-8">
          <p className="mb-2 text-sm font-bold uppercase tracking-[0.18em] text-coral">
            Start moving
          </p>
          <h1 className="font-display text-3xl sm:text-4xl font-bold text-ink">
            Create your account.
          </h1>
          <p className="mt-2 text-ink/60 text-sm">
            Join the SwiftDrop network to send, deliver, or fulfill orders seamlessly.
          </p>
        </div>

        {/* Signup form & options */}
        <div className="space-y-6">
          {/* Step 1: Role Selection */}
          <div className="space-y-2">
            <label htmlFor="role" className="block text-sm font-semibold text-ink/80">
              I am joining as
            </label>
            <select
              id="role"
              className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm font-medium text-ink outline-none transition focus:border-coral focus:ring-2 focus:ring-coral/20"
              {...register("role")}
            >
              <option value="CUSTOMER">Customer (Send & Receive Parcels)</option>
              <option value="MERCHANT">Merchant (Store & Business Fulfillment)</option>
              <option value="COURIER">Courier (Delivery Driver / Rider)</option>
            </select>
            {errors.role && (
              <span className="text-xs text-coral">{errors.role.message}</span>
            )}

            {/* Dynamic helper badge */}
            <div className="pt-1">
              {selectedRole === "CUSTOMER" ? (
                <div className="flex items-center gap-2 rounded-xl bg-emerald-50 border border-emerald-200/80 p-3 text-xs text-emerald-800">
                  <Sparkles size={16} className="text-emerald-600 shrink-0" />
                  <span>
                    <strong>Instant Access:</strong> Customers can book, track deliveries, and enter the dashboard immediately without admin review.
                  </span>
                </div>
              ) : (
                <div className="flex items-center gap-2 rounded-xl bg-amber-50 border border-amber-200/80 p-3 text-xs text-amber-900">
                  <ShieldAlert size={16} className="text-amber-600 shrink-0" />
                  <span>
                    <strong>Admin Review Required:</strong> {selectedRole === "MERCHANT" ? "Merchant" : "Courier"} accounts require manual Admin approval before accessing their dashboard.
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Sign up with Google Button passing chosen role */}
          <div>
            <GoogleLoginButton
              role={selectedRole}
              text={`Sign up with Google as ${
                selectedRole === "CUSTOMER"
                  ? "Customer"
                  : selectedRole === "MERCHANT"
                  ? "Merchant"
                  : "Courier"
              }`}
            />
          </div>

          <div className="relative my-6 flex items-center justify-center">
            <div className="w-full border-t border-ink/10" />
            <span className="absolute bg-[#fbfcfa] px-3 text-xs font-semibold text-ink/50 uppercase tracking-wider">
              or register with email
            </span>
          </div>

          <form onSubmit={handleSubmit(submit)} className="grid gap-4 sm:grid-cols-2">
            <FormField
              label="Full name"
              placeholder="Alex Morgan"
              error={errors.name?.message}
              {...register("name")}
            />

            <FormField
              label="Email address"
              type="email"
              placeholder="you@example.com"
              error={errors.email?.message}
              {...register("email")}
            />

            <div className="sm:col-span-2">
              <FormField
                label="Password"
                type="password"
                placeholder="At least 8 characters"
                error={errors.password?.message}
                {...register("password")}
              />
            </div>

            {serverError && (
              <p className="sm:col-span-2 rounded-xl bg-coral/10 border border-coral/20 px-3.5 py-2.5 text-sm text-coral">
                {serverError}
              </p>
            )}

            <button
              disabled={isLoading}
              type="submit"
              className="sm:col-span-2 mt-2 flex items-center justify-center gap-2 rounded-xl bg-ink px-4 py-3.5 font-semibold text-white transition hover:bg-ink/90 disabled:opacity-60 shadow-md"
            >
              {isLoading ? "Creating account..." : "Create account"}
              <ArrowRight size={18} />
            </button>
          </form>
        </div>

        <p className="mt-6 text-center text-sm text-ink/60">
          Already have access?{" "}
          <Link to="/login" className="font-bold text-coral hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
