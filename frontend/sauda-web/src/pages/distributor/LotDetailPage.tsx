import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  fetchDistributorLotMatch,
  updateLotMatchStatus,
} from "../../api/lotMatches";
import { useAuth } from "../../auth/AuthProvider";
import { LotDetailView } from "../../components/distributor/LotDetailView";
import type { DistributorLotMatchCard, LotMatchStatus } from "../../types/api";
import { ApiError } from "../../api/client";

export function LotDetailPage() {
  const { matchId } = useParams<{ matchId: string }>();
  const navigate = useNavigate();
  const { token, user } = useAuth();

  const [match, setMatch] = useState<DistributorLotMatchCard | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    if (!token || !user?.organizationId || !matchId) return;
    setLoading(true);
    setError("");
    try {
      const data = await fetchDistributorLotMatch(user.organizationId, matchId, token);
      setMatch(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Лот не найден");
    } finally {
      setLoading(false);
    }
  }, [token, user?.organizationId, matchId]);

  useEffect(() => {
    load();
  }, [load]);

  async function handleStatusChange(status: LotMatchStatus) {
    if (!token || !user?.organizationId || !matchId) return;
    setActionLoading(true);
    try {
      const updated = await updateLotMatchStatus(
        user.organizationId,
        matchId,
        token,
        status,
      );
      setMatch(updated);
      if (status === "dismissed") {
        navigate("/lots");
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось обновить статус");
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="py-24 text-center text-slate-500">Загрузка…</div>
    );
  }

  if (error || !match) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-24 text-center">
        <p className="text-red-600">{error || "Лот не найден"}</p>
      </div>
    );
  }

  return (
    <LotDetailView
      match={match}
      onStatusChange={handleStatusChange}
      actionLoading={actionLoading}
    />
  );
}
