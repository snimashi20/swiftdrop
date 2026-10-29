import { STATUS_BADGE, STATUS_LABEL } from '../statusMeta';
import type { ParcelStatus } from '../types';

export default function StatusBadge({ status }: { status: ParcelStatus }) {
  return (
    <span
      className={`inline-block rounded-full px-3 py-1 text-sm font-semibold ${STATUS_BADGE[status]}`}
    >
      {STATUS_LABEL[status]}
    </span>
  );
}
