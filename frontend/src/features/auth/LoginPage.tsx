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
      await login(data);
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
        <a
          href="/oauth2/authorization/google"
          className="flex w-full items-center justify-center gap-3 rounded-xl border border-ink/15 bg-white px-4 py-3.5 font-semibold text-ink shadow-sm transition hover:bg-ink/[0.03]"
        >
          <svg className="h-5 w-5" viewBox="0 0 24 24">
            <path
              fill="#4285F4"
              d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            />
            <path
              fill="#34A853"
              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            />
            <path
              fill="#FBBC05"
              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
            />
            <path
              fill="#EA4335"
              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
            />
          </svg>
          Continue with Google
        </a>

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