import { useCallback, useEffect, useState } from "react";
import { listParsedRows } from "../../../../api/imports";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import type { ParsedRowResponse, ParsedRowStatus } from "../types";

const PAGE_SIZE = 50;

export function useParsedRows(
  runId: string | undefined,
  statusFilter: ParsedRowStatus | "",
) {
  const { token, user } = useAuth();
  const [items, setItems] = useState<ParsedRowResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(
    async (silent = false) => {
      if (!token || !user?.organizationId || !runId) return;
      if (!silent) {
        setLoading(true);
        setError("");
      }
      try {
        const response = await listParsedRows(user.organizationId, runId, token, {
          page,
          size: PAGE_SIZE,
          status: statusFilter || undefined,
        });
        setItems(response.content);
        setTotalPages(response.totalPages);
        setTotalElements(response.totalElements);
      } catch (err) {
        if (!silent) {
          setError(err instanceof ApiError ? err.message : "Ошибка загрузки строк");
        }
      } finally {
        if (!silent) setLoading(false);
      }
    },
    [token, user?.organizationId, runId, page, statusFilter],
  );

  useEffect(() => {
    setPage(0);
  }, [statusFilter, runId]);

  useEffect(() => {
    reload();
  }, [reload]);

  return {
    items,
    page,
    totalPages,
    totalElements,
    loading,
    error,
    setPage,
    reload,
  };
}
