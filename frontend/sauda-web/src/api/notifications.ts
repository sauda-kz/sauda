import type { PageResponse } from "../types/api";
import { apiRequest } from "./client";

export type NotificationStatus = "unread" | "read";

export interface InternalNotification {
  id: string;
  title: string;
  message: string;
  status: NotificationStatus;
  lotMatchId: string;
  lotId: string;
  lotTitle: string;
  createdAt: string;
  readAt: string | null;
}

export function fetchNotifications(
  token: string,
  params?: { status?: NotificationStatus; page?: number; size?: number },
) {
  const search = new URLSearchParams();
  if (params?.status) search.set("status", params.status);
  if (params?.page !== undefined) search.set("page", String(params.page));
  if (params?.size !== undefined) search.set("size", String(params.size));
  const query = search.toString();
  return apiRequest<PageResponse<InternalNotification>>(
    `/notifications${query ? `?${query}` : ""}`,
    { token },
  );
}

export function fetchUnreadCount(token: string) {
  return apiRequest<number>("/notifications/unread-count", { token });
}

export function markNotificationRead(notificationId: string, token: string) {
  return apiRequest<InternalNotification>(`/notifications/${notificationId}/read`, {
    method: "PATCH",
    token,
  });
}
