import { ComingSoon } from "../components/ComingSoon";
import { PageHeader } from "../components/PageHeader";

export function CouriersPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Operations desk"
        title="Couriers"
        text="Courier profiles and how busy each courier is."
      />
      <ComingSoon
        heading="Courier list and profile form"
        items={[
          "Courier list with zone, vehicle and active status",
          "Number of active deliveries per courier",
          "Set zone and vehicle, mark a courier active or inactive",
        ]}
      />
    </div>
  );
}