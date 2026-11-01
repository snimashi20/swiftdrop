import { ComingSoon } from "../components/ComingSoon";
import { PageHeader } from "../components/PageHeader";

export function NotificationLogPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Admin"
        title="Notification log"
        text="Every e-mail and mock SMS the notification service has sent."
      />
      <ComingSoon
        heading="Log table"
        items={[
          "Reads the notifications API of the notification service",
          "Channel, status and time of each message",
          "Phone numbers shown masked",
        ]}
      />
    </div>
  );
}