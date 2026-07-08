import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../../../auth/AuthProvider";
import { fetchPotentialMatches } from "../../../../api/lots";
import { ApiError } from "../../../../api/client";
import type { PotentialMatch } from "../types";

export function usePotentialMatches(lotId: string | undefined) {
  const { token } = useAuth();
  const [matches, setMatches] = useState<PotentialMatch[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    if (!token || !lotId) return;
    setLoading(true);
    setError("");
    try {
      setMatches(await fetchPotentialMatches(lotId, token));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка загрузки совпадений");
    } finally {
      setLoading(false);
    }
  }, [token, lotId]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { matches, loading, error, reload };
}
