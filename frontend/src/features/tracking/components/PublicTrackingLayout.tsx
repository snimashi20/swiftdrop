import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';

/** Light layout for the public tracking pages: no sidebar, no login needed. */
export default function PublicTrackingLayout() {
  const { user } = useAuth();
  return (
    <div className="min-h-screen bg-mist text-ink">
      <header className="flex h-16 items-center justify-between border-b border-ink/10 px-4 md:px-8">
        <Link to="/track" className="font-display text-2xl font-bold tracking-tight">
          swift<span className="text-coral">drop</span>
        </Link>
        <Link
          to={user ? '/dashboard' : '/login'}
          className="rounded-xl px-4 py-2 text-sm font-medium text-ink/70 hover:bg-ink/5"
        >
          {user ? 'My dashboard' : 'Sign in'}
        </Link>
      </header>
      <main className="mx-auto max-w-5xl p-4 md:p-8">
        <Outlet />
      </main>
    </div>
  );
}
