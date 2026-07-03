import { useAuth } from "../../../../auth/AuthProvider";
import { fetchLots } from "../../../../api/lots";
import type { LotsListParams, PageResponse, Lot } from "../types";
import { ApiError } from "../../../../api/client";
import { useCallback, useEffect, useState } from "react";

export function useLots(params: LotsListParams) {
  const { token } = useAuth();
  const [page, setPage] = useState<PageResponse<Lot> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const { q, status, category, source, page: pageNum, size } = params;

  const reload = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      const result = await fetchLots(token, { q, status, category, source, page: pageNum, size });
      setPage(result);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка загрузки");
    } finally {
      setLoading(false);
    }
  }, [token, q, status, category, source, pageNum, size]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { page, loading, error, reload };
}
