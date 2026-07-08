import { useEffect } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { markNotificationRead } from "../../../../api/notifications";
import { useAuth } from "../../../../auth/AuthProvider";
import { useDistributorPermissions } from "../../hooks/useDistributorPermissions";
import { SuitableLotDetailView } from "../components/SuitableLotDetailView";
import { useSuitableLotDetail } from "../hooks/useSuitableLotDetail";

export function SuitableLotDetailPage() {
  const { matchId } = useParams<{ matchId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { token } = useAuth();
  const { canManageMatches } = useDistributorPermissions();
  const { match, loading, actionLoading, error, updateStatus } =
    useSuitableLotDetail(matchId);

  const notificationId = searchParams.get("notificationId");

  useEffect(() => {
    if (!token || !notificationId) return;
    markNotificationRead(notificationId, token).catch(() => undefined);
  }, [token, notificationId]);

  async function handleStatusChange(
    status: import("../../../../types/api").LotMatchStatus,
  ) {
    const updated = await updateStatus(status);
    if (updated && status === "dismissed") {
      navigate("/suitable-lots");
    }
  }

  if (loading) {
    return <div className="py-24 text-center text-slate-500">Загрузка…</div>;
  }

  if (error || !match) {
    return (
      <div className="py-24 text-center">
        <p className="text-red-600">{error || "Лот не найден"}</p>
      </div>
    );
  }

  return (
    <SuitableLotDetailView
      match={match}
      canManage={canManageMatches}
      actionLoading={actionLoading}
      onStatusChange={handleStatusChange}
    />
  );
}
