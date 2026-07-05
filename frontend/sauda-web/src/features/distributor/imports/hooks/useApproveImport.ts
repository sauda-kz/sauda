import { useState } from "react";
import { approveImport } from "../../../../api/imports";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import type { ImportRunResponse } from "../types";

export function useApproveImport(runId: string | undefined, onSuccess?: () => void) {
  const { token, user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function approve(): Promise<ImportRunResponse | null> {
    if (!token || !user?.organizationId || !runId) return null;
    setLoading(true);
    setError("");
    try {
      const updated = await approveImport(user.organizationId, runId, token);
      onSuccess?.();
      return updated;
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось подтвердить импорт");
      return null;
    } finally {
      setLoading(false);
    }
  }

  return { approve, loading, error, clearError: () => setError("") };
}
