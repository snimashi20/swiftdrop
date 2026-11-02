import { ComingSoon } from "../components/ComingSoon";
import { PageHeader } from "../components/PageHeader";

export function ReportsPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Operations desk"
        title="Reports"
        text="Delivery numbers for the last 7 and 30 days."
      />
      <ComingSoon
        heading="Charts"
        items={[
          "Deliveries per day (last 7 and 30 days)",
          "Success, failure and cancelled counts",
          "Deliveries per zone",
        ]}
      />
    </div>
  );
}