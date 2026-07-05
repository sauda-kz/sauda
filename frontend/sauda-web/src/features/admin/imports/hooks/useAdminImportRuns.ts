import { useCallback, useEffect, useState } from "react";
import { listAdminImportRuns } from "../../../../api/imports";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import type { ImportRunResponse, ImportStatus } from "../../../distributor/imports/types";

const PAGE_SIZE = 20;

export interface AdminImportRunsFilters {
  distributorId?: string;
  status?: ImportStatus | "";
  page?: number;
}

export function useAdminImportRuns(filters: AdminImportRunsFilters) {
  const { token } = useAuth();
  const [items, setItems] = useState<ImportRunResponse[]>([]);
  const [page, setPage] = useState(filters.page ?? 0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      const response = await listAdminImportRuns(token, {
        page,
        size: PAGE_SIZE,
        distributorId: filters.distributorId || undefined,
        status: filters.status || undefined,
      });
      setItems(response.content);
      setTotalPages(response.totalPages);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка загрузки импортов");
    } finally {
      setLoading(false);
    }
  }, [token, page, filters.distributorId, filters.status]);

  useEffect(() => {
    setPage(0);
  }, [filters.distributorId, filters.status]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { items, page, totalPages, loading, error, setPage, reload };
}
