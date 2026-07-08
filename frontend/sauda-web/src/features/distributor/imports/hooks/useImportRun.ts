import { useCallback, useEffect, useRef, useState } from "react";
import { getImportRun } from "../../../../api/imports";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import { PROCESSING_IMPORT_STATUSES, type ImportRunResponse } from "../types";

const POLL_INTERVAL_MS = 3000;

export function useImportRun(runId: string | undefined) {
  const { token, user } = useAuth();
  const [run, setRun] = useState<ImportRunResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const pollingRef = useRef<number | null>(null);

  const reload = useCallback(
    async (silent = false) => {
      if (!token || !user?.organizationId || !runId) return null;
      if (!silent) {
        setLoading(true);
        setError("");
      }
      try {
        const response = await getImportRun(user.organizationId, runId, token);
        setRun(response);
        return response;
      } catch (err) {
        if (!silent) {
          setError(err instanceof ApiError ? err.message : "Импорт не найден");
        }
        return null;
      } finally {
        if (!silent) setLoading(false);
      }
    },
    [token, user?.organizationId, runId],
  );

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    if (pollingRef.current != null) {
      window.clearInterval(pollingRef.current);
      pollingRef.current = null;
    }

    if (!run || !PROCESSING_IMPORT_STATUSES.includes(run.status)) {
      return;
    }

    pollingRef.current = window.setInterval(() => {
      reload(true);
    }, POLL_INTERVAL_MS);

    return () => {
      if (pollingRef.current != null) {
        window.clearInterval(pollingRef.current);
      }
    };
  }, [run?.id, run?.status, reload]);

  const isProcessing = run != null && PROCESSING_IMPORT_STATUSES.includes(run.status);

  return { run, loading, error, reload, isProcessing };
}
