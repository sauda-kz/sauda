import { useEffect, useState } from "react";
import { Link, Outlet, useLocation } from "react-router-dom";
import { LogOut, Menu, User, X } from "lucide-react";
import { useAuth } from "../../auth/AuthProvider";
import { Logo } from "../ui/Logo";
import { NotificationBell } from "../../features/distributor/notifications/components/NotificationBell";
import { useDistributorPermissions } from "../../features/distributor/hooks/useDistributorPermissions";
import { Footer } from "./Footer";

export function DistributorLayout() {
  const { organization, logout } = useAuth();
  const { canReadImports, canReadOffers } = useDistributorPermissions();
  const location = useLocation();
  const orgName = organization?.name ?? "Компания";
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  const items: [string, string][] = [
    ["/suitable-lots", "Подходящие лоты"],
    ...(canReadOffers ? ([["/offers", "Мой прайс"]] as [string, string][]) : []),
    ...(canReadImports ? ([["/imports", "Импорты"]] as [string, string][]) : []),
    ["/notifications", "Уведомления"],
  ];

  const navLink = (to: string, label: string) => {
    const active = location.pathname.startsWith(to);
    return (
      <Link
        key={to}
        to={to}
        aria-current={active ? "page" : undefined}
        className={`relative inline-flex h-16 items-center text-sm font-medium transition-colors ${
          active ? "text-brand-700" : "text-slate-600 hover:text-slate-900"
        }`}
      >
        {label}
        {active && (
          <span className="absolute inset-x-0 -bottom-px h-0.5 rounded-full bg-brand-600" />
        )}
      </Link>
    );
  };

  return (
    <div className="flex min-h-dvh flex-col bg-slate-50">
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/85 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-8">
            <button
              type="button"
              onClick={() => setMenuOpen((v) => !v)}
              aria-label="Меню"
              aria-expanded={menuOpen}
              className="-ml-1 inline-flex h-9 w-9 cursor-pointer items-center justify-center rounded-lg text-slate-600 transition-colors hover:bg-slate-100 hover:text-slate-900 sm:hidden"
            >
              {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
            <Link to="/suitable-lots">
              <Logo />
            </Link>
            <nav className="hidden items-center gap-7 sm:flex">
              {items.map(([to, label]) => navLink(to, label))}
            </nav>
          </div>

          <div className="flex items-center gap-2 sm:gap-3">
            <NotificationBell />

            <div className="flex items-center gap-2 rounded-full border border-slate-200 bg-white py-1 pl-1 pr-3 shadow-xs">
              <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-100 text-brand-700">
                <User className="h-4 w-4" />
              </span>
              <span className="hidden max-w-[140px] truncate text-sm font-medium text-slate-700 sm:block">
                {orgName}
              </span>
            </div>

            <button
              type="button"
              onClick={logout}
              className="inline-flex h-9 cursor-pointer items-center gap-1.5 rounded-lg px-2.5 text-sm font-medium text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900"
            >
              <LogOut className="h-4 w-4" />
              <span className="hidden sm:block">Выйти</span>
            </button>
          </div>
        </div>

        {menuOpen && (
          <nav className="border-t border-slate-100 bg-white px-2 py-2 sm:hidden">
            {items.map(([to, label]) => {
              const active = location.pathname.startsWith(to);
              return (
                <Link
                  key={to}
                  to={to}
                  aria-current={active ? "page" : undefined}
                  className={`block rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                    active
                      ? "bg-brand-50 text-brand-700"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  {label}
                </Link>
              );
            })}
          </nav>
        )}
      </header>
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
