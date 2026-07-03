import { Link, Outlet } from "react-router-dom";
import { useAuth } from "../../auth/AuthProvider";

export function AdminLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-8">
            <Link to="/admin/lots" className="flex items-center gap-2.5">
              <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-sm font-bold text-white">
                S
              </span>
              <span className="text-xl font-bold text-slate-900">Sauda Admin</span>
            </Link>
            <nav className="hidden sm:block">
              <Link
                to="/admin/lots"
                className="text-sm font-medium text-brand-600"
              >
                Лоты
              </Link>
            </nav>
          </div>
          <div className="flex items-center gap-4">
            <span className="hidden text-sm text-slate-500 sm:block">{user?.email}</span>
            <button
              type="button"
              onClick={logout}
              className="text-sm text-slate-500 hover:text-slate-700"
            >
              Выйти
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
}
