import { Link, Outlet, useLocation } from "react-router-dom";
import { ChevronDown, User } from "lucide-react";
import { useAuth } from "../../auth/AuthProvider";
import { NotificationBell } from "../../features/distributor/notifications/components/NotificationBell";
import { Footer } from "./Footer";

function SaudaLogo() {
  return (
    <Link to="/suitable-lots" className="flex items-center gap-2.5">
      <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-sm font-bold text-white">
        S
      </span>
      <span className="text-xl font-bold tracking-tight text-slate-900">Sauda</span>
    </Link>
  );
}

export function DistributorLayout() {
  const { organization, logout } = useAuth();
  const location = useLocation();
  const orgName = organization?.name ?? "Компания";

  const navLink = (to: string, label: string) => {
    const active = location.pathname.startsWith(to);
    return (
      <Link
        to={to}
        className={`text-sm font-medium ${active ? "text-brand-600" : "text-slate-600 hover:text-brand-600"}`}
      >
        {label}
      </Link>
    );
  };

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-8">
            <SaudaLogo />
            <nav className="hidden items-center gap-6 sm:flex">
              {navLink("/suitable-lots", "Подходящие лоты")}
              {navLink("/notifications", "Уведомления")}
            </nav>
          </div>

          <div className="flex items-center gap-3">
            <NotificationBell />

            <div className="flex items-center gap-2 rounded-lg border border-slate-200 py-1.5 pl-2 pr-3">
              <span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-brand-600">
                <User className="h-4 w-4" />
              </span>
              <span className="max-w-[120px] truncate text-sm font-medium text-slate-700">
                {orgName.length > 12 ? `${orgName.slice(0, 10)}…` : orgName}
              </span>
              <ChevronDown className="h-4 w-4 text-slate-400" />
            </div>

            <button
              type="button"
              onClick={logout}
              className="hidden text-sm text-slate-500 hover:text-slate-700 sm:block"
            >
              Выйти
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
