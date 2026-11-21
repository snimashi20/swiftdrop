import { Home } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getApiErrorMessage } from "../../../lib/api";
import { fetchUserAddresses } from "../../profile/profile.api";
import type { Address } from "../../profile/profile.api";
import { BookingHeader } from "./BookingHeader";
import { useBookingDraft } from "./BookingDraft";
import { findDistrict } from "./placeAddress";

// The customer's saved addresses (from the address book). Picking one fills the address.
export function SavedAddressesPage() {
  const navigate = useNavigate();
  const { target, setChosen } = useBookingDraft();
  const [addresses, setAddresses] = useState<Address[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchUserAddresses()
      .then(setAddresses)
      .catch((err) => setError(getApiErrorMessage(err, "Could not load your saved addresses.")));
  }, []);

  const choose = (a: Address) => {
    const district = findDistrict(a.district);
    if (!district) return;
    setChosen(target, { address: { line1: a.streetAddress, city: a.city, district }, position: null });
    navigate("/parcels/new");
  };

  return (
    <div className="mx-auto max-w-3xl">
      <BookingHeader
        title="Saved addresses"
        subtitle="Pick one from your address book."
        onBack={() => navigate("/parcels/new/location")}
        action={
          <Link to="/profile/addresses" className="rounded-xl border border-ink/15 px-4 py-2 text-sm font-semibold hover:bg-mist">
            Add address
          </Link>
        }
      />

      {error && <p className="mb-4 rounded-lg border border-coral/30 bg-coral/10 p-3 text-sm">{error}</p>}
      {addresses && addresses.length === 0 && (
        <p className="rounded-2xl border border-ink/5 bg-white p-6 text-center text-sm text-ink/60 shadow-sm">
          You have no saved addresses yet. Use <b>Add</b> to save one.
        </p>
      )}

      {addresses && addresses.length > 0 && (
        <ul className="divide-y divide-ink/10 overflow-hidden rounded-2xl border border-ink/5 bg-white shadow-sm">
          {addresses.map((a) => {
            const deliverable = findDistrict(a.district) !== null;
            return (
              <li key={a.id}>
                <button
                  type="button"
                  disabled={!deliverable}
                  onClick={() => choose(a)}
                  className="flex w-full items-center gap-4 px-4 py-4 text-left hover:bg-mist disabled:opacity-50"
                >
                  <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-coral text-white">
                    <Home className="h-5 w-5" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-semibold">{a.addressLabel ?? a.label ?? "Address"}</span>
                    <span className="block truncate text-xs text-ink/55">
                      {a.streetAddress}, {a.city}
                    </span>
                    {!deliverable && <span className="block text-xs text-coral">We do not deliver to {a.district} yet</span>}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
