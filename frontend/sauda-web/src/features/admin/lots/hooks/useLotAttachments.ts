import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../../../auth/AuthProvider";
import {
  deleteLotAttachment,
  fetchLotAttachments,
  uploadLotAttachment,
} from "../../../../api/lotAttachments";
import { ApiError } from "../../../../api/client";
import type { LotAttachment } from "../types";

export function useLotAttachments(lotId: string | undefined) {
  const { token } = useAuth();
  const [attachments, setAttachments] = useState<LotAttachment[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    if (!token || !lotId) return;
    setLoading(true);
    setError("");
    try {
      setAttachments(await fetchLotAttachments(lotId, token));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка загрузки документов");
    } finally {
      setLoading(false);
    }
  }, [token, lotId]);

  useEffect(() => {
    reload();
  }, [reload]);

  const upload = useCallback(
    async (file: File) => {
      if (!token || !lotId) return;
      setUploading(true);
      setError("");
      try {
        await uploadLotAttachment(lotId, file, token);
        await reload();
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Не удалось загрузить файл");
        throw err;
      } finally {
        setUploading(false);
      }
    },
    [token, lotId, reload],
  );

  const remove = useCallback(
    async (attachmentId: string) => {
      if (!token || !lotId) return;
      setError("");
      try {
        await deleteLotAttachment(lotId, attachmentId, token);
        await reload();
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Не удалось удалить файл");
      }
    },
    [token, lotId, reload],
  );

  return { attachments, loading, uploading, error, reload, upload, remove };
}
