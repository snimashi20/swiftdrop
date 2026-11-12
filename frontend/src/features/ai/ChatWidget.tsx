import { useState } from 'react';

interface Message { role: 'user' | 'assistant'; content: string; }

export const ChatWidget = () => {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);

  const send = async () => {
    if (!input.trim() || loading) return;
    const userMsg: Message = { role: 'user', content: input };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const baseUrl = import.meta.env.VITE_AI_BASE_URL || 'http://localhost:8083';
      const res = await fetch(`${baseUrl}/api/v1/ai/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: input }),
      });
      const data = await res.json();
      setMessages(prev => [...prev, { role: 'assistant', content: data.reply }]);
    } catch {
      setMessages(prev => [...prev, { role: 'assistant', content: 'Sorry, I could not reach the assistant.' }]);
    } finally {
      setLoading(false);
    }
  };

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        className="fixed bottom-6 right-6 bg-coral text-white rounded-full w-14 h-14 shadow-lg flex items-center justify-center text-2xl hover:bg-coral/90 z-50"
        aria-label="Open chat"
      >
        💬
      </button>
    );
  }

  return (
    <div className="fixed bottom-6 right-6 w-96 h-[500px] bg-white rounded-2xl shadow-2xl border border-ink/10 flex flex-col z-50">
      <div className="flex justify-between items-center p-4 border-b border-ink/10">
        <h3 className="font-display font-bold text-ink">SwiftDrop Assistant</h3>
        <button onClick={() => setOpen(false)} className="text-ink/60 hover:text-ink text-lg" aria-label="Close chat">✕</button>
      </div>
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {messages.length === 0 && (
          <p className="text-sm text-ink/50 italic">
            Try: "Where is my parcel SD7K2M9QX4LP?" or "How much to send 2kg from Colombo to Kandy?"
          </p>
        )}
        {messages.map((m, i) => (
          <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[80%] rounded-2xl px-3 py-2 text-sm whitespace-pre-wrap ${
              m.role === 'user' ? 'bg-coral text-white' : 'bg-mist text-ink'
            }`}>
              {m.content}
            </div>
          </div>
        ))}
        {loading && <div className="text-sm text-ink/50 italic">Thinking...</div>}
      </div>
      <div className="p-3 border-t border-ink/10 flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && send()}
          placeholder="Ask a question..."
          className="flex-1 rounded-xl border border-ink/15 px-3 py-2 text-sm"
        />
        <button onClick={send} disabled={loading} className="bg-coral text-white rounded-xl px-4 text-sm font-semibold disabled:opacity-60">
          Send
        </button>
      </div>
    </div>
  );
};
