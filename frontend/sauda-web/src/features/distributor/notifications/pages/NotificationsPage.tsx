import { Link } from "react-router-dom";
import { useNotifications } from "../hooks/useNotifications";

export function NotificationsPage() {
  const { items, loading, error, markRead } = useNotifications();

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Уведомления</h1>
        <p className="mt-1 text-sm text-slate-500">Новые подходящие лоты для вашей компании</p>
      </div>

      {loading && <p className="text-sm text-slate-500">Загрузка…</p>}
      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}

      {!loading && !error && items.length === 0 && (
        <p className="rounded-xl border border-dashed border-slate-200 bg-white py-12 text-center text-sm text-slate-500">
          Нет уведомлений
        </p>
      )}

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
    </div>
  );
}
