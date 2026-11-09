import { useState, useEffect } from 'react';
import { Zone, PricingRule, fetchZones, fetchRules, createZone, updateRule } from '../api/adminPricingApi';

export const AdminPricingPage = () => {
  const [zones, setZones] = useState<Zone[]>([]);
  const [rules, setRules] = useState<PricingRule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [newZoneName, setNewZoneName] = useState('');
  const [newZoneDistrict, setNewZoneDistrict] = useState('');

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
      loadData();
    } catch (err) {
      alert('Failed to create zone.');
    }
  };

  const handleUpdateRule = async (id: number, field: keyof PricingRule, value: number) => {
    try {
      await updateRule(id, { [field]: value });
      loadData();
    } catch (err) {
      alert('Failed to update rule.');
    }
  };

  if (loading) return <div className="p-8 text-center text-ink/50 font-sans">Loading pricing data...</div>;
  if (error) return <div className="p-8 text-center text-coral font-sans">{error}</div>;

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-8 font-sans">
      <h1 className="text-2xl font-display font-bold text-ink">Zones & Pricing</h1>

      {/* Zones Section */}
      <div className="bg-mist p-6 rounded-lg shadow border border-ink/10">
        <h2 className="text-xl font-display font-semibold text-ink mb-4">Zones</h2>
        
        <form onSubmit={handleCreateZone} className="flex gap-4 mb-6">
          <input
            type="text"
            placeholder="Zone Name (e.g., Colombo Zone)"
            value={newZoneName}
            onChange={(e) => setNewZoneName(e.target.value)}
            className="border border-ink/20 rounded px-3 py-2 flex-1 bg-white text-ink focus:outline-none focus:border-coral"
            required
          />
          <input
            type="text"
            placeholder="District (e.g., Colombo)"
            value={newZoneDistrict}
            onChange={(e) => setNewZoneDistrict(e.target.value)}
            className="border border-ink/20 rounded px-3 py-2 flex-1 bg-white text-ink focus:outline-none focus:border-coral"
            required
          />
          <button type="submit" className="bg-coral text-white px-4 py-2 rounded hover:bg-coral/90 transition font-medium">
            Add Zone
          </button>
        </form>

        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-ink/10 text-ink/70 text-sm">
              <th className="p-3 font-medium">ID</th>
              <th className="p-3 font-medium">Name</th>
              <th className="p-3 font-medium">District</th>
              <th className="p-3 font-medium">Status</th>
            </tr>
          </thead>
          <tbody>
            {zones.map((zone) => (
              <tr key={zone.id} className="border-b border-ink/5 hover:bg-ink/5">
                <td className="p-3 text-ink/60">{zone.id}</td>
                <td className="p-3 font-medium text-ink">{zone.name}</td>
                <td className="p-3 text-ink/70">{zone.district}</td>
                <td className="p-3">
                  <span className={`px-2 py-1 rounded text-xs font-bold ${zone.isActive ? 'bg-mint text-ink' : 'bg-ink/10 text-ink/60'}`}>
                    {zone.isActive ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pricing Rules Section */}
      <div className="bg-mist p-6 rounded-lg shadow border border-ink/10">
        <h2 className="text-xl font-display font-semibold text-ink mb-4">Pricing Rules</h2>
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-ink/10 text-ink/70 text-sm">
              <th className="p-3 font-medium">Zone</th>
              <th className="p-3 font-medium">Base Fee (Rs.)</th>
              <th className="p-3 font-medium">Per Kg (Rs.)</th>
              <th className="p-3 font-medium">Inter-Zone Surcharge (Rs.)</th>
            </tr>
          </thead>
          <tbody>
            {rules.map((rule) => (
              <tr key={rule.id} className="border-b border-ink/5 hover:bg-ink/5">
                <td className="p-3 font-medium text-ink">{rule.zoneName}</td>
                <td className="p-3">
                  <input 
                    type="number" 
                    defaultValue={rule.baseFee} 
                    onBlur={(e) => handleUpdateRule(rule.id, 'baseFee', parseFloat(e.target.value))}
                    className="border border-ink/20 rounded px-2 py-1 w-24 bg-white text-ink focus:outline-none focus:border-coral"
                  />
                </td>
                <td className="p-3">
                  <input 
                    type="number" 
                    defaultValue={rule.perKgFee} 
                    onBlur={(e) => handleUpdateRule(rule.id, 'perKgFee', parseFloat(e.target.value))}
                    className="border border-ink/20 rounded px-2 py-1 w-24 bg-white text-ink focus:outline-none focus:border-coral"
                  />
                </td>
                <td className="p-3">
                  <input 
                    type="number" 
                    defaultValue={rule.interZoneSurcharge} 
                    onBlur={(e) => handleUpdateRule(rule.id, 'interZoneSurcharge', parseFloat(e.target.value))}
                    className="border border-ink/20 rounded px-2 py-1 w-24 bg-white text-ink focus:outline-none focus:border-coral"
                  />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="text-sm text-ink/50 mt-2">* Edit values and click outside the box to save automatically.</p>
      </div>
    </div>
  );
};
