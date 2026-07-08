import { Link } from "react-router-dom";
import { Bell, ChevronDown, User } from "lucide-react";
import { useAuth } from "../../auth/AuthProvider";

function SaudaLogo() {
  return (
    <Link to="/" className="flex items-center gap-2.5">
      <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-sm font-bold text-white">
        S
      </span>
      <span className="text-xl font-bold tracking-tight text-slate-900">Sauda</span>
    </Link>
  );
}

export function Header() {
  const { organization, logout } = useAuth();
  const orgName = organization?.name ?? "Компания";

  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-8">
          <SaudaLogo />
          <nav className="hidden sm:block">
            <Link
              to="/lots"
              className="text-sm font-medium text-brand-600"
            >
              Лоты
            </Link>
          </nav>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            className="relative rounded-lg p-2 text-slate-500 hover:bg-slate-100"
            aria-label="Уведомления"
          >
            <Bell className="h-5 w-5" />
            <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-red-500" />
          </button>

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
  );
}
