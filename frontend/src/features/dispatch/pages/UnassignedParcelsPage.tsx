import { ComingSoon } from "../components/ComingSoon";
import { PageHeader } from "../components/PageHeader";

export function UnassignedParcelsPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Operations desk"
        title="Unassigned parcels"
        text="Parcels that are waiting for a courier."
      />
      <ComingSoon
        heading="Table with an Assign dialog"
        items={[
          "Filter by district and date",
          "Assign dialog showing each courier's workload and the suggested courier",
          "Reassign a parcel that already has a courier",
        ]}
      />
    </div>
  );
}