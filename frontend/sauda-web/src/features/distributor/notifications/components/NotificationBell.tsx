import { Link } from "react-router-dom";
import { Bell } from "lucide-react";
import { useNotifications } from "../hooks/useNotifications";
import { useDistributorPermissions } from "../../hooks/useDistributorPermissions";

export function NotificationBell() {
  const { canReadNotifications } = useDistributorPermissions();
  const { unreadCount } = useNotifications();

  if (!canReadNotifications) return null;

  return (
    <Link
      to="/notifications"
      className="relative rounded-lg p-2 text-slate-500 hover:bg-slate-100"
      aria-label="Уведомления"
    >
      <Bell className="h-5 w-5" />
      {unreadCount > 0 && (
        <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
          {unreadCount > 99 ? "99+" : unreadCount}
        </span>
      )}
    </Link>
  );
}
