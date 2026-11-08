import { useEffect, useState } from "react";
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { getApiErrorMessage } from "../../../lib/api";
import { dispatchApi } from "../api";
import { PageHeader } from "../components/PageHeader";
import type { ReportSummary } from "../types";

const COLORS = { delivered: "#22C55E", failed: "#EF4444", cancelled: "#6B7280" };

type StatCardProps = { label: string; value: string; hint?: string };

function StatCard({ label, value, hint }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-ink/10 bg-white p-5">
      <p className="text-sm text-ink/50">{label}</p>
      <p className="mt-3 font-display text-4xl font-bold">{value}</p>
      {hint && <p className="mt-1 text-xs text-ink/40">{hint}</p>}
    </div>
  );
}

const percent = (value: number | null) => (value === null ? "—" : `${value}%`);

export function ReportsPage() {
  const [days, setDays] = useState<7 | 30>(7);
  const [report, setReport] = useState<ReportSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError("");
    dispatchApi
      .reportSummary(days)
      .then((data) => {
        if (!ignore) setReport(data);
      })
      .catch((err) => {
        if (!ignore) setError(getApiErrorMessage(err, "Could not load the report."));
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [days]);

  const daily = (report?.daily ?? []).map((d) => ({ ...d, label: d.date.slice(5) }));
  const hasData = !!report && (report.outcomes.delivered + report.outcomes.failed + report.outcomes.cancelled > 0);

  return (
    <div>
      <PageHeader
        eyebrow="Operations desk"
        title="Reports"
        text="Parcels that reached a final status, by day and by pickup zone."
      />

      <div className="mb-6 inline-flex rounded-xl border border-ink/10 bg-white p-1">
        {([7, 30] as const).map((option) => (
          <button
            key={option}
            onClick={() => setDays(option)}
            className={`rounded-lg px-4 py-2 text-sm font-semibold transition ${
              days === option ? "bg-ink text-white" : "text-ink/60 hover:text-ink"
            }`}
          >
            Last {option} days
          </button>
        ))}
      </div>

      {loading && <p className="text-sm text-ink/50">Loading report…</p>}
      {error && <p className="rounded-lg bg-coral/10 px-3 py-2 text-sm text-coral">{error}</p>}

      {report && !loading && !error && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Delivered" value={String(report.outcomes.delivered)} />
            <StatCard label="Failed" value={String(report.outcomes.failed)} />
            <StatCard label="Cancelled" value={String(report.outcomes.cancelled)} hint="Not counted in the rates" />
            <StatCard
              label="Success rate"
              value={percent(report.outcomes.successRatePercent)}
              hint={`Failure rate ${percent(report.outcomes.failureRatePercent)}`}
            />
          </div>

          {!hasData && (
            <p className="rounded-2xl border border-dashed border-ink/20 bg-white p-6 text-sm text-ink/55">
              No finished deliveries in this period yet.
            </p>
          )}

          <section className="rounded-2xl border border-ink/10 bg-white p-5">
            <h2 className="mb-4 font-display text-xl font-bold">Deliveries per day</h2>
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={daily}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="label" fontSize={12} />
                  <YAxis allowDecimals={false} fontSize={12} />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="delivered" name="Delivered" stackId="day" fill={COLORS.delivered} />
                  <Bar dataKey="failed" name="Failed" stackId="day" fill={COLORS.failed} />
                  <Bar dataKey="cancelled" name="Cancelled" stackId="day" fill={COLORS.cancelled} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </section>

          <section className="rounded-2xl border border-ink/10 bg-white p-5">
            <h2 className="mb-4 font-display text-xl font-bold">Deliveries per zone</h2>
            {report.perZone.length === 0 ? (
              <p className="text-sm text-ink/50">No zone data for this period.</p>
            ) : (
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={report.perZone}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="district" fontSize={12} />
                    <YAxis allowDecimals={false} fontSize={12} />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="delivered" name="Delivered" stackId="zone" fill={COLORS.delivered} />
                    <Bar dataKey="failed" name="Failed" stackId="zone" fill={COLORS.failed} />
                    <Bar dataKey="cancelled" name="Cancelled" stackId="zone" fill={COLORS.cancelled} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}