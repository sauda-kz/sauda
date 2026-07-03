import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../../../../auth/AuthProvider";
import { fetchLot } from "../../../../api/lots";
import { ApiError } from "../../../../api/client";
import type { Lot } from "../types";

export function useLot(lotId: string | undefined) {
  const { token } = useAuth();
  const [lot, setLot] = useState<Lot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    if (!token || !lotId) return;
    setLoading(true);
    setError("");
    try {
      setLot(await fetchLot(lotId, token));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Лот не найден");
    } finally {
      setLoading(false);
    }
  }, [token, lotId]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { lot, loading, error, reload };
}
