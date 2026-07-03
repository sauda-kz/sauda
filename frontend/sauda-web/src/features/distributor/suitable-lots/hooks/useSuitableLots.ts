import { useCallback, useEffect, useState } from "react";
import { fetchDistributorLotMatches } from "../../../../api/lotMatches";
import { useAuth } from "../../../../auth/AuthProvider";
import { ApiError } from "../../../../api/client";
import type { DistributorLotMatchCard } from "../../../../types/api";

export function useSuitableLots() {
  const { token, user } = useAuth();
  const [items, setItems] = useState<DistributorLotMatchCard[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    if (!token || !user?.organizationId) return;
    setLoading(true);
    setError("");
    try {
      const page = await fetchDistributorLotMatches(user.organizationId, token, {
        page: 0,
        size: 50,
      });
      setItems(page.content);
      setTotal(page.totalElements);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка загрузки");
    } finally {
      setLoading(false);
    }
  }, [token, user?.organizationId]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { items, total, loading, error, reload };
}
