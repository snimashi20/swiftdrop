import { Link, useParams } from 'react-router-dom';
import LiveParcelView from './components/LiveParcelView';
import PageHeader from './components/PageHeader';

/** Public page /track/:trackingNumber (no login). */
export default function TrackingPage() {
  const { trackingNumber = '' } = useParams<{ trackingNumber: string }>();

  return (
    <div className="space-y-5">
      <PageHeader title="Tracking details" subtitle="Real-time updates of your shipment" />
      <Link to="/track" className="inline-block text-sm font-medium text-ink/70 hover:text-ink hover:underline">
        ← Track another parcel
      </Link>
      <LiveParcelView trackingNumber={trackingNumber.toUpperCase()} />
    </div>
  );
}
