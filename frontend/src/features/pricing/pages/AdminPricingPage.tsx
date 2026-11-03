import { useState, useEffect } from 'react';
import { Zone, PricingRule, fetchZones, fetchRules, createZone, updateRule } from '../api/adminPricingApi';

export const AdminPricingPage = () => {
  const [zones, setZones] = useState<Zone[]>([]);
  const [rules, setRules] = useState<PricingRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state for new zone
  const [newZoneName, setNewZoneName] = useState('');
  const [newZoneDistrict, setNewZoneDistrict] = useState('');

  // Load data
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [zData, rData] = await Promise.all([fetchZones(), fetchRules()]);
      setZones(zData);
      setRules(rData);
    } catch (err) {
      setError('Failed to load pricing data.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateZone = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createZone({ name: newZoneName, district: newZoneDistrict });
      setNewZoneName('');
      setNewZoneDistrict('');
      loadData(); // Refresh list
    } catch (err) {
      alert('Failed to create zone.');
    }
  };

  const handleUpdateRule = async (id: number, field: keyof PricingRule, value: number) => {
    try {
      await updateRule(id, { [field]: value });
      loadData(); // Refresh list to show new values
    } catch (err) {
      alert('Failed to update rule.');
    }
  };

  if (loading) return <div className="p-8 text-center text-gray-500">Loading pricing data...</div>;
  if (error) return <div className="p-8 text-center text-red-500">{error}</div>;

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-8">
      <h1 className="text-2xl font-bold text-gray-800">Zones & Pricing</h1>

      {/* Zones Section */}
      <div className="bg-white p-6 rounded-lg shadow border border-gray-200">
        <h2 className="text-xl font-semibold mb-4">Zones</h2>
        
        {/* Add Zone Form */}
        <form onSubmit={handleCreateZone} className="flex gap-4 mb-6">
          <input
            type="text"
            placeholder="Zone Name (e.g., Colombo Zone)"
            value={newZoneName}
            onChange={(e) => setNewZoneName(e.target.value)}
            className="border border-gray-300 rounded px-3 py-2 flex-1"
            required
          />
          <input
            type="text"
            placeholder="District (e.g., Colombo)"
            value={newZoneDistrict}
            onChange={(e) => setNewZoneDistrict(e.target.value)}
            className="border border-gray-300 rounded px-3 py-2 flex-1"
            required
          />
          <button type="submit" className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
            Add Zone
          </button>
        </form>

        {/* Zones Table */}
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-gray-100 text-gray-700">
              <th className="p-3 border-b">ID</th>
              <th className="p-3 border-b">Name</th>
              <th className="p-3 border-b">District</th>
              <th className="p-3 border-b">Status</th>
            </tr>
          </thead>
          <tbody>
            {zones.map((zone) => (
              <tr key={zone.id} className="border-b hover:bg-gray-50">
                <td className="p-3">{zone.id}</td>
                <td className="p-3 font-medium">{zone.name}</td>
                <td className="p-3">{zone.district}</td>
                <td className="p-3">
                  <span className={`px-2 py-1 rounded text-xs font-bold ${zone.isActive ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                    {zone.isActive ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pricing Rules Section */}
      <div className="bg-white p-6 rounded-lg shadow border border-gray-200">
        <h2 className="text-xl font-semibold mb-4">Pricing Rules</h2>
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-gray-100 text-gray-700">
              <th className="p-3 border-b">Zone</th>
              <th className="p-3 border-b">Base Fee (Rs.)</th>
              <th className="p-3 border-b">Per Kg (Rs.)</th>
              <th className="p-3 border-b">Inter-Zone Surcharge (Rs.)</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((rule) => (
              <tr key={rule.id} className="border-b hover:bg-gray-50">
                <td className="p-3 font-medium">{rule.zoneName}</td>
                <td className="p-3">
                  <input 
                    type="number" 
                    defaultValue={rule.baseFee} 
                    onBlur={(e) => handleUpdateRule(rule.id, 'baseFee', parseFloat(e.target.value))}
                    className="border border-gray-300 rounded px-2 py-1 w-24"
                  />
                </td>
                <td className="p-3">
                  <input 
                    type="number" 
                    defaultValue={rule.perKgFee} 
                    onBlur={(e) => handleUpdateRule(rule.id, 'perKgFee', parseFloat(e.target.value))}
                    className="border border-gray-300 rounded px-2 py-1 w-24"
                  />
                </td>
                <td className="p-3">
                  <input 
                    type="number" 
                    defaultValue={rule.interZoneSurcharge} 
                    onBlur={(e) => handleUpdateRule(rule.id, 'interZoneSurcharge', parseFloat(e.target.value))}
                    className="border border-gray-300 rounded px-2 py-1 w-24"
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="text-sm text-gray-500 mt-2">* Edit values and click outside the box to save automatically.</p>
      </div>
    </div>
  );
};
