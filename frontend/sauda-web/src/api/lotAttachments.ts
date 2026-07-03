import type { LotAttachment } from "../features/admin/lots/types";
import { ApiError } from "./client";

const API_ROOT = import.meta.env.VITE_API_URL ?? "/api";
const API_BASE = `${API_ROOT}/v1`;

export function fetchLotAttachments(lotId: string, token: string) {
  return fetch(`${API_BASE}/lots/${lotId}/attachments`, {
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`,
    },
  }).then(async (response) => {
    const text = await response.text();
    const parsed = text ? (JSON.parse(text) as LotAttachment[]) : [];
    if (!response.ok) {
      throw new ApiError(response.status, "Failed to load attachments");
    }
    return parsed;
  });
}

export function uploadLotAttachment(lotId: string, file: File, token: string) {
  const formData = new FormData();
  formData.append("file", file);

  return fetch(`${API_BASE}/lots/${lotId}/attachments`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  }).then(async (response) => {
    const text = await response.text();
    const parsed = text ? (JSON.parse(text) as LotAttachment) : null;
    if (!response.ok) {
      const body = parsed as { message?: string } | null;
      throw new ApiError(
        response.status,
        body?.message ?? `Upload failed (${response.status})`,
      );
    }
    return parsed as LotAttachment;
  });
}

export function deleteLotAttachment(
  lotId: string,
  attachmentId: string,
  token: string,
) {
  return fetch(`${API_BASE}/lots/${lotId}/attachments/${attachmentId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  }).then((response) => {
    if (!response.ok) {
      throw new ApiError(response.status, "Failed to delete attachment");
    }
  });
}

export function attachmentDownloadUrl(lotId: string, attachmentId: string): string {
  return `${API_BASE}/lots/${lotId}/attachments/${attachmentId}/download`;
}
