import type { LucideIcon } from "lucide-react";
import { Bell, CircleCheck, Package, Truck } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import { PageHeader } from "../components/PageHeader";

type Section = {
  to: string;
  title: string;
  text: string;
  icon: LucideIcon;
  adminOnly?: boolean;
};

const sections: Section[] = [
  {
    to: "/fleet/unassigned",
    title: "Unassigned parcels",
    text: "See parcels waiting for a courier and assign them.",
    icon: Package,
  },
  {
    to: "/fleet/couriers",
    title: "Couriers",
    text: "Manage zones, vehicles and who is active, and see each courier's workload.",
    icon: Truck,
  },
  {
    to: "/fleet/reports",
    title: "Reports",
    text: "Deliveries per day, success and failure rate, and deliveries per zone.",
    icon: CircleCheck,
  },
  {
    to: "/fleet/notifications",
    title: "Notification log",
    text: "E-mails and mock SMS messages sent to customers.",
    icon: Bell,
    adminOnly: true,
  },
];

export function DispatcherDashboardPage() {
  const { user } = useAuth();
  const visible = sections.filter((section) => !section.adminOnly || user?.role === "ADMIN");

  return (
    <div>
      <PageHeader
        eyebrow="Operations desk"
        title="Fleet & dispatch"
        text="Assign parcels to couriers, keep an eye on workloads and follow the delivery numbers."
        showBack={false}
      />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {visible.map(({ to, title, text, icon: Icon }) => (
          <Link
            key={to}
            to={to}
            className="group rounded-2xl border border-ink/10 bg-white p-6 transition hover:border-coral/40 hover:shadow-lg"
          >
            <div className="mb-6 text-ink/45 group-hover:text-coral">
              <Icon size={22} />
            </div>
            <h2 className="font-display text-xl font-bold">{title}</h2>
            <p className="mt-2 text-sm leading-6 text-ink/55">{text}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}