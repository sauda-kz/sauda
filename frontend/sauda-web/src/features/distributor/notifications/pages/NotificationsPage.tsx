import { Link } from "react-router-dom";
import { BellOff } from "lucide-react";
import { Spinner } from "../../../../components/ui/Spinner";
import { useNotifications } from "../hooks/useNotifications";

export function NotificationsPage() {
  const { items, loading, error, markRead } = useNotifications();

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Уведомления</h1>
        <p className="mt-1 text-sm text-slate-500">Новые подходящие лоты для вашей компании</p>
      </div>

      {loading && <Spinner label="Загрузка уведомлений…" />}
      {error && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}

      {!loading && !error && items.length === 0 && (
        <div className="flex flex-col items-center rounded-xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
            <BellOff className="h-6 w-6" />
          </span>
          <p className="mt-4 text-sm font-medium text-slate-900">Пока нет уведомлений</p>
          <p className="mt-1 max-w-sm text-sm text-slate-500">
            Здесь появятся оповещения о новых подходящих лотах для вашей компании.
          </p>
        </div>
      )}

      {!loading && !error && items.length > 0 && (
      <ul className="divide-y divide-slate-100 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        {items.map((n) => (
          <li key={n.id}>
            <Link
              to={`/suitable-lots/${n.lotMatchId}?notificationId=${n.id}`}
              onClick={() => {
                if (n.status === "unread") void markRead(n.id);
              }}
              className={`block px-5 py-4 hover:bg-slate-50 ${n.status === "unread" ? "bg-brand-50/40" : ""}`}
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-medium text-slate-900">{n.title}</p>
                  <p className="mt-1 text-sm text-slate-600">{n.message}</p>
                  {n.lotTitle && (
                    <p className="mt-2 text-xs font-medium text-brand-600">{n.lotTitle}</p>
                  )}
                </div>
                {n.status === "unread" && (
                  <span className="mt-1 h-2 w-2 shrink-0 rounded-full bg-brand-500" />
                )}
              </div>
              <p className="mt-2 text-xs text-slate-400">
                {new Date(n.createdAt).toLocaleString("ru-RU")}
              </p>
            </Link>
          </li>
        ))}
      </ul>
      )}
    </div>
  );
}
