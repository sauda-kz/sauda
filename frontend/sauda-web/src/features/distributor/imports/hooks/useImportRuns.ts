import { useCallback, useEffect, useState } from "react";
import { listImportRuns } from "../../../../api/imports";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import type { ImportRunResponse } from "../types";

const PAGE_SIZE = 20;

export function useImportRuns() {
  const { token, user } = useAuth();
  const [items, setItems] = useState<ImportRunResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    if (!token || !user?.organizationId) return;
    setLoading(true);
    setError("");
    try {
      const response = await listImportRuns(user.organizationId, token, {
        page,
        size: PAGE_SIZE,
      });
      setItems(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка загрузки импортов");
    } finally {
      setLoading(false);
    }
  }, [token, user?.organizationId, page]);

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
