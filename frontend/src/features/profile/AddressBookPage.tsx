import React, { useEffect, useState } from "react";
import {
  fetchUserAddresses,
  createUserAddress,
  deleteUserAddress,
  Address,
} from "./profile.api";

export const AddressBookPage: React.FC = () => {
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Form input states
  const [label, setLabel] = useState("Home");
  const [streetAddress, setStreetAddress] = useState("");
  const [city, setCity] = useState("");
  const [district, setDistrict] = useState("Colombo");
  const [postalCode, setPostalCode] = useState("");

  const loadAddresses = () => {
    setLoading(true);
    fetchUserAddresses()
      .then((data) => {
        setAddresses(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Failed to load addresses.");
        setLoading(false);
      });
  };

  useEffect(() => {
    loadAddresses();
  }, []);

  const handleAddAddress = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      await createUserAddress({
        addressLabel: label,
        streetAddress,
        city,
        district,
        postalCode: postalCode || undefined,
        isDefault: addresses.length === 0,
      });
      setSuccess("Address saved successfully!");
      setStreetAddress("");
      setCity("");
      setPostalCode("");
      loadAddresses();
    } catch (err) {
      console.error(err);
      setError("Failed to save address. Please check all fields.");
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm("Are you sure you want to delete this address?")) return;
    try {
      await deleteUserAddress(id);
      loadAddresses();
    } catch (err) {
      console.error(err);
      setError("Failed to delete address.");
    }
  };

  return (
    <div className="max-w-3xl mx-auto p-6 bg-white rounded-2xl shadow-sm border border-ink/5 mt-6">
      <h1 className="text-2xl font-bold mb-6 text-ink">Saved Address Book</h1>

      {error && <div className="mb-4 p-3 bg-rose-50 text-rose-800 text-sm rounded-lg border border-rose-200">{error}</div>}
      {success && <div className="mb-4 p-3 bg-emerald-50 text-emerald-800 text-sm rounded-lg border border-emerald-200">{success}</div>}

      {/* Address List */}
      <div className="mb-8">
        <h2 className="text-lg font-semibold mb-3 text-ink/80">Your Saved Addresses</h2>
        {loading ? (
          <div className="flex justify-center p-6">
            <div className="w-6 h-6 border-2 border-coral border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : addresses.length === 0 ? (
          <p className="text-ink/50 italic py-4">No saved addresses found.</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {addresses.map((addr) => (
              <div
                key={addr.id}
                className="p-4 border border-ink/10 rounded-xl bg-ink/[0.02] relative flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="inline-block px-2.5 py-0.5 text-xs font-semibold text-coral bg-coral/10 rounded-md">
                      {addr.addressLabel || addr.label || "Saved"}
                    </span>
                    {addr.isDefault && (
                      <span className="text-[11px] font-medium text-ink/60 bg-ink/5 px-2 py-0.5 rounded">
                        Default
                      </span>
                    )}
                  </div>
                  <p className="text-sm font-medium text-ink">{addr.streetAddress}</p>
                  <p className="text-sm text-ink/70">
                    {addr.city}, {addr.district}
                  </p>
                  {addr.postalCode && <p className="text-xs text-ink/50 mt-1">Postal: {addr.postalCode}</p>}
                </div>
                <button
                  type="button"
                  onClick={() => handleDelete(addr.id)}
                  className="mt-3 text-xs text-rose-600 hover:text-rose-800 self-end font-semibold transition"
                >
                  Delete
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <hr className="my-6 border-ink/10" />

      {/* Add New Address Form */}
      <div>
        <h2 className="text-lg font-semibold mb-4 text-ink/80">Add New Address</h2>
        <form onSubmit={handleAddAddress} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-ink/70">Label</label>
              <select
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                className="w-full mt-1.5 p-3 border border-ink/15 rounded-xl text-ink bg-white focus:outline-none focus:ring-2 focus:ring-coral/20"
              >
                <option value="Home">Home</option>
                <option value="Office">Office</option>
                <option value="Warehouse">Warehouse</option>
                <option value="Other">Other</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-ink/70">District</label>
              <input
                type="text"
                value={district}
                onChange={(e) => setDistrict(e.target.value)}
                required
                className="w-full mt-1.5 p-3 border border-ink/15 rounded-xl text-ink focus:outline-none focus:ring-2 focus:ring-coral/20"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-ink/70">City</label>
              <input
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                required
                className="w-full mt-1.5 p-3 border border-ink/15 rounded-xl text-ink focus:outline-none focus:ring-2 focus:ring-coral/20"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-ink/70">Postal Code (Optional)</label>
              <input
                type="text"
                value={postalCode}
                onChange={(e) => setPostalCode(e.target.value)}
                className="w-full mt-1.5 p-3 border border-ink/15 rounded-xl text-ink focus:outline-none focus:ring-2 focus:ring-coral/20"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-ink/70">Street Address</label>
            <input
              type="text"
              value={streetAddress}
              onChange={(e) => setStreetAddress(e.target.value)}
              required
              placeholder="123 Galle Road"
              className="w-full mt-1.5 p-3 border border-ink/15 rounded-xl text-ink focus:outline-none focus:ring-2 focus:ring-coral/20"
            />
          </div>

          <button
            type="submit"
            className="w-full bg-coral text-white py-3 rounded-xl font-semibold hover:bg-coral/90 transition shadow-sm"
          >
            Save Address
          </button>
        </form>
      </div>
    </div>
  );
};
