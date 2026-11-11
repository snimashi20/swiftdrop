import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { FormField } from "../../components/FormField";
import { getApiErrorMessage } from "../../lib/api";
import { useAuth } from "./AuthContext";
import { AuthFrame } from "./AuthFrame";
import { GoogleLoginButton } from "./GoogleLoginButton";
import type { LoginInput } from "./auth.types";

const schema = z.object({
  email: z.string().email("Enter a valid email address"),
  password: z.string().min(8, "Use at least 8 characters"),
  remember: z.boolean(),
});

export function LoginPage() {
  const { login, isLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [serverError, setServerError] = useState("");

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginInput>({
    resolver: zodResolver(schema),
    defaultValues: { remember: true },
  });

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const errorParam = params.get("error");
    if (errorParam) {
      if (errorParam === "account_disabled") {
        setServerError("Your account has been deactivated. Please contact support.");
      } else if (errorParam === "email_not_verified") {
        setServerError("Your Google account email is not verified.");
      } else if (errorParam === "missing_email") {
        setServerError("Google sign-in did not provide an email address.");
      } else {
        setServerError("Authentication failed. Please try again.");
      }
    }
  }, [location.search]);

  const submit = async (data: LoginInput) => {
    setServerError("");
    try {
      const user = await login(data);
      if (user.status === "PENDING_ROLE") {
        navigate("/select-role", { replace: true });
        return;
      }
      if (user.status === "PENDING_APPROVAL") {
        navigate("/waiting-approval", { replace: true });
        return;
      }
      navigate((location.state as { from?: string })?.from || "/dashboard", {
        replace: true,
      });
    } catch (error) {
      setServerError(getApiErrorMessage(error, "We could not sign you in."));
    }
  };

  return (
    <AuthFrame title="Welcome back." subtitle="Your delivery network, in one clear view.">
      <div className="space-y-4">
        {/* Continue with Google Button */}
        <GoogleLoginButton />

        <div className="relative my-4 flex items-center justify-center">
          <div className="w-full border-t border-ink/10" />
          <span className="absolute bg-white px-3 text-xs font-medium text-ink/50 uppercase tracking-wider">
            or continue with email
          </span>
        </div>

        <form onSubmit={handleSubmit(submit)} className="space-y-4">
          <FormField
            label="Email address"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            error={errors.email?.message}
            {...register("email")}
          />
          <FormField
            label="Password"
            type="password"
            autoComplete="current-password"
            placeholder="Your password"
            error={errors.password?.message}
            {...register("password")}
          />

          {serverError && (
            <p className="rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">
              {serverError}
            </p>
          )}

          <label className="flex items-center gap-2 text-sm text-ink/60 cursor-pointer">
            <input type="checkbox" className="accent-coral" {...register("remember")} />
            Keep me signed in
          </label>

          <button
            disabled={isLoading}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-ink px-4 py-3.5 font-semibold text-white transition hover:bg-ink/90 disabled:opacity-60"
          >
            {isLoading ? "Signing in..." : "Sign in"}
            <ArrowRight size={18} />
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-ink/60">
          New to SwiftDrop?{" "}
          <Link to="/signup" className="font-bold text-coral hover:underline">
            Create an account
          </Link>
        </p>
      </div>
    </AuthFrame>
  );
}