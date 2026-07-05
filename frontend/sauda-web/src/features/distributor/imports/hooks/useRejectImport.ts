import { useState } from "react";
import { rejectImport } from "../../../../api/imports";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import type { ImportRunResponse } from "../types";

export function useRejectImport(runId: string | undefined, onSuccess?: () => void) {
  const { token, user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function reject(reason?: string): Promise<ImportRunResponse | null> {
    if (!token || !user?.organizationId || !runId) return null;
    setLoading(true);
    setError("");
    try {
      const updated = await rejectImport(user.organizationId, runId, token, reason);
      onSuccess?.();
      return updated;
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось отклонить импорт");
      return null;
    } finally {
      setLoading(false);
    }
  }

  return { reject, loading, error, clearError: () => setError("") };
}
