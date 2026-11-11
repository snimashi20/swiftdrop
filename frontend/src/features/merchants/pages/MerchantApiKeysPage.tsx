import { Check, Copy, KeyRound, Plus, ShieldAlert, Trash2 } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { getApiErrorMessage } from "../../../lib/api";
import { createApiKey, fetchApiKeys, fetchMyMerchant, revokeApiKey } from "../api";
import type { ApiKey, GeneratedApiKey, Merchant } from "../types";

export function MerchantApiKeysPage() {
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [label, setLabel] = useState("");
  const [generated, setGenerated] = useState<GeneratedApiKey | null>(null);
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [profile, apiKeys] = await Promise.all([fetchMyMerchant(), fetchApiKeys()]);
      setMerchant(profile);
      setKeys(apiKeys);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Could not load API keys."));
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { void load(); }, [load]);

  const generate = async (event: React.FormEvent) => {
    event.preventDefault();
    setError("");
    setWorking(true);
    try {
      const result = await createApiKey(label.trim());
      setGenerated(result);
      setLabel("");
      setCopied(false);
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Could not create an API key."));
    } finally {
      setWorking(false);
    }
  };

  const copy = async () => {
    if (!generated) return;
    await navigator.clipboard.writeText(generated.apiKey);
    setCopied(true);
  };

  const revoke = async (key: ApiKey) => {
    if (!window.confirm(`Revoke “${key.label}”? Requests using this key will stop working.`)) return;
    setWorking(true);
    setError("");
    try {
      await revokeApiKey(key.id);
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Could not revoke this key."));
    } finally {
      setWorking(false);
    }
  };

  return <section className="mx-auto max-w-4xl space-y-6 font-sans text-ink">
    <header><p className="text-xs font-bold uppercase tracking-[0.2em] text-coral">Merchant workspace</p><h1 className="mt-2 font-display text-3xl font-bold sm:text-4xl">API keys</h1><p className="mt-2 text-sm text-ink/55">Create keys for your store integration and revoke any key that is no longer in use.</p></header>
    {error && <p role="alert" className="rounded-xl bg-coral/10 px-4 py-3 text-sm text-coral">{error}</p>}
    {generated && <div className="rounded-2xl border border-coral/20 bg-white p-5">
      <div className="flex gap-3"><ShieldAlert className="shrink-0 text-coral" /><div><h2 className="font-semibold">Copy your new key now</h2><p className="mt-1 text-sm text-ink/55">SwiftDrop will not show the full key again. Store it securely in your application secrets.</p></div></div>
      <div className="mt-4 flex flex-col gap-2 sm:flex-row"><code className="min-w-0 flex-1 break-all rounded-xl bg-mist p-3 text-sm">{generated.apiKey}</code><button onClick={() => void copy()} className="inline-flex items-center justify-center gap-2 rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-white">{copied ? <Check size={16} /> : <Copy size={16} />}{copied ? "Copied" : "Copy key"}</button></div>
      <button onClick={() => setGenerated(null)} className="mt-3 text-sm font-semibold text-ink/60 underline">I saved the key</button>
    </div>}
    {merchant?.status !== "APPROVED" && merchant && <div className="rounded-2xl border border-ink/10 bg-white p-5"><p className="font-semibold">Your merchant account is {merchant.status.toLowerCase()}.</p><p className="mt-1 text-sm text-ink/55">API keys become available after an administrator approves your business.</p></div>}
    {merchant?.status === "APPROVED" && <form onSubmit={generate} className="flex flex-col gap-3 rounded-2xl border border-ink/10 bg-white p-5 sm:flex-row sm:items-end">
      <label className="flex-1 space-y-1.5"><span className="text-sm font-semibold">Key label</span><input value={label} onChange={(event) => setLabel(event.target.value)} maxLength={100} required placeholder="Production store" className="w-full rounded-xl border border-ink/15 bg-white px-3.5 py-3 text-sm outline-none focus:border-coral" /></label>
      <button disabled={working} className="inline-flex items-center justify-center gap-2 rounded-xl bg-coral px-5 py-3 font-semibold text-white disabled:opacity-60"><Plus size={17} />Generate key</button>
    </form>}
    <div className="overflow-hidden rounded-2xl border border-ink/10 bg-white">
      <div className="border-b border-ink/10 p-5"><h2 className="font-display text-xl font-bold">Your keys</h2></div>
      {loading ? <p className="p-8 text-center text-sm text-ink/55">Loading keys…</p> : keys.length === 0 ? <p className="p-8 text-center text-sm text-ink/55">No API keys have been created yet.</p> : <div className="divide-y divide-ink/10">
        {keys.map((key) => <div key={key.id} className="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between"><div className="flex min-w-0 items-start gap-3"><div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-mint"><KeyRound size={18} /></div><div className="min-w-0"><p className="font-semibold">{key.label}</p><code className="text-sm text-ink/55">{key.keyPrefix}••••••••</code><p className="mt-1 text-xs text-ink/40">Created {new Date(key.createdAt).toLocaleDateString()}{key.lastUsedAt ? ` · Last used ${new Date(key.lastUsedAt).toLocaleDateString()}` : " · Not used yet"}</p></div></div>{key.active ? <button disabled={working} onClick={() => void revoke(key)} className="inline-flex items-center justify-center gap-2 rounded-xl border border-coral/25 px-4 py-2.5 text-sm font-semibold text-coral disabled:opacity-50"><Trash2 size={15} />Revoke</button> : <span className="self-start rounded-full bg-ink/5 px-3 py-1 text-xs font-semibold text-ink/45">Revoked</span>}</div>)}
      </div>}
    </div>
  </section>;
}
