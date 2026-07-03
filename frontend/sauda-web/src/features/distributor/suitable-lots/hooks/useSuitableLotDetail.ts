import { useCallback, useEffect, useState } from "react";
import {
  fetchDistributorLotMatch,
  updateLotMatchStatus,
} from "../../../../api/lotMatches";
import { useAuth } from "../../../../auth/AuthProvider";
import { ApiError } from "../../../../api/client";
import type { DistributorLotMatchCard, LotMatchStatus } from "../../../../types/api";

export function useSuitableLotDetail(matchId: string | undefined) {
  const { token, user } = useAuth();
  const [match, setMatch] = useState<DistributorLotMatchCard | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    if (!token || !user?.organizationId || !matchId) return;
    setLoading(true);
    setError("");
    try {
      setMatch(await fetchDistributorLotMatch(user.organizationId, matchId, token));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Лот не найден");
    } finally {
      setLoading(false);
    }
  }, [token, user?.organizationId, matchId]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function updateStatus(status: LotMatchStatus, distributorComment?: string) {
    if (!token || !user?.organizationId || !matchId) return null;
    setActionLoading(true);
    setError("");
    try {
      const updated = await updateLotMatchStatus(
        user.organizationId,
        matchId,
        token,
        status,
        distributorComment,
      );
      setMatch(updated);
      return updated;
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось обновить статус");
      return null;
    } finally {
      setActionLoading(false);
    }
  }

  return { match, loading, actionLoading, error, reload, updateStatus };
}
