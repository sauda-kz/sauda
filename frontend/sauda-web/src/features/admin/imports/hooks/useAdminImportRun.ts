import { useCallback, useEffect, useRef, useState } from "react";
import { getImportRun, listParsedRows } from "../../../../api/imports";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import {
  PROCESSING_IMPORT_STATUSES,
  type ImportRunResponse,
  type ParsedRowResponse,
  type ParsedRowStatus,
} from "../../../distributor/imports/types";

const POLL_INTERVAL_MS = 3000;
const PAGE_SIZE = 50;

export function useAdminImportRun(distributorId: string | undefined, runId: string | undefined) {
  const { token } = useAuth();
  const [run, setRun] = useState<ImportRunResponse | null>(null);
  const [rows, setRows] = useState<ParsedRowResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<ParsedRowStatus | "">("");
  const [loading, setLoading] = useState(true);
  const [rowsLoading, setRowsLoading] = useState(true);
  const [error, setError] = useState("");
  const [rowsError, setRowsError] = useState("");
  const pollingRef = useRef<number | null>(null);

  const reloadRun = useCallback(
    async (silent = false) => {
      if (!token || !distributorId || !runId) return null;
      if (!silent) {
        setLoading(true);
        setError("");
      }
      try {
        const response = await getImportRun(distributorId, runId, token);
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
    [token, distributorId, runId],
  );

  const reloadRows = useCallback(
    async (silent = false) => {
      if (!token || !distributorId || !runId) return;
      if (!silent) {
        setRowsLoading(true);
        setRowsError("");
      }
      try {
        const response = await listParsedRows(distributorId, runId, token, {
          page: 0,
          size: PAGE_SIZE,
          status: statusFilter || undefined,
        });
        setRows(response.content);
      } catch (err) {
        if (!silent) {
          setRowsError(err instanceof ApiError ? err.message : "Ошибка загрузки строк");
        }
      } finally {
        if (!silent) setRowsLoading(false);
      }
    },
    [token, distributorId, runId, statusFilter],
  );

  useEffect(() => {
    reloadRun();
    reloadRows();
  }, [reloadRun, reloadRows]);

  const isProcessing = run != null && PROCESSING_IMPORT_STATUSES.includes(run.status);

  useEffect(() => {
    if (pollingRef.current != null) {
      window.clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
    if (!isProcessing) return;

    pollingRef.current = window.setInterval(() => {
      reloadRun(true);
      reloadRows(true);
    }, POLL_INTERVAL_MS);

    return () => {
      if (pollingRef.current != null) {
        window.clearInterval(pollingRef.current);
      }
    };
  }, [isProcessing, reloadRun, reloadRows]);

  return {
    run,
    rows,
    loading,
    rowsLoading,
    error,
    rowsError,
    statusFilter,
    setStatusFilter,
    isProcessing,
    reloadRun,
    reloadRows,
  };
}
