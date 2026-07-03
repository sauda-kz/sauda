import { useCallback, useEffect, useState } from "react";
import {
  fetchNotifications,
  fetchUnreadCount,
  markNotificationRead,
  type InternalNotification,
  type NotificationStatus,
} from "../../../../api/notifications";
import { useAuth } from "../../../../auth/AuthProvider";
import { ApiError } from "../../../../api/client";

export function useNotifications(status?: NotificationStatus) {
  const { token } = useAuth();
  const [items, setItems] = useState<InternalNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      const [page, count] = await Promise.all([
        fetchNotifications(token, { status, page: 0, size: 50 }),
        fetchUnreadCount(token),
      ]);
      setItems(page.content);
      setUnreadCount(count);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка загрузки уведомлений");
    } finally {
      setLoading(false);
    }
  }, [token, status]);

  useEffect(() => {
    reload();
  }, [reload]);

  const markRead = useCallback(
    async (notificationId: string) => {
      if (!token) return;
      await markNotificationRead(notificationId, token);
      await reload();
    },
    [token, reload],
  );

  return { items, unreadCount, loading, error, reload, markRead };
}
