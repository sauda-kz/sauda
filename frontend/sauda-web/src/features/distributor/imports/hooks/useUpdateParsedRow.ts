import { useState } from "react";
import { updateParsedRow } from "../../../../api/imports";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import type { ParsedRowResponse, UpdateParsedRowRequest } from "../types";

export function useUpdateParsedRow(runId: string | undefined, onSuccess?: () => void) {
  const { token, user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function update(
    rowId: string,
    payload: UpdateParsedRowRequest,
  ): Promise<ParsedRowResponse | null> {
    if (!token || !user?.organizationId || !runId) return null;
    setLoading(true);
    setError("");
    try {
      const updated = await updateParsedRow(
        user.organizationId,
        runId,
        rowId,
        token,
        payload,
      );
      onSuccess?.();
      return updated;
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось сохранить строку");
      return null;
    } finally {
      setLoading(false);
    }
  }

  return { update, loading, error, clearError: () => setError("") };
}
