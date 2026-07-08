import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { fetchLotMatches } from "../../../../api/lots";
import { useAuth } from "../../../../auth/AuthProvider";
import { Badge } from "../../../../components/ui/Badge";
import { Button } from "../../../../components/ui/Button";
import { formatMoney } from "../../../../utils/format";
import { LotAttachmentsSection } from "../components/LotAttachmentsSection";
import { LotStatusBadge } from "../components/LotStatusBadge";
import { PotentialMatchesPanel } from "../components/PotentialMatchesPanel";
import { useLot } from "../hooks/useLot";
import { usePotentialMatches } from "../hooks/usePotentialMatches";
import type { AdminLotMatch } from "../types";

export function LotDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { token } = useAuth();
  const { lot, loading, error, reload: reloadLot } = useLot(id);
  const {
    matches: potentialMatches,
    loading: potentialLoading,
    error: potentialError,
    reload: reloadPotential,
  } = usePotentialMatches(id);
  const [matches, setMatches] = useState<AdminLotMatch[]>([]);
  const [matchesLoading, setMatchesLoading] = useState(true);

  const reloadMatches = useCallback(async () => {
    if (!token || !id) return;
    setMatchesLoading(true);
    try {
      const page = await fetchLotMatches(id, token);
      setMatches(page.content);
    } finally {
      setMatchesLoading(false);
    }
  }, [token, id]);

  useEffect(() => {
    reloadMatches();
  }, [reloadMatches]);

  function handleSent() {
    reloadPotential();
    reloadMatches();
    reloadLot();
  }

  if (loading) {
    return <p className="text-sm text-slate-500">Загрузка…</p>;
  }
  if (error || !lot) {
    return <p className="text-sm text-red-600">{error || "Лот не найден"}</p>;
  }

  const needsReviewHighlight =
    lot.status === "needs_review" || lot.dataQualityStatus === "incomplete";

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link to="/admin/lots" className="text-sm text-brand-600 hover:underline">
            ← К списку лотов
          </Link>
          <h1
            className={`mt-2 text-2xl font-bold ${needsReviewHighlight ? "text-amber-800" : "text-slate-900"}`}
          >
            {lot.title}
          </h1>
          <p className="mt-1 text-slate-600">{lot.customerName}</p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <LotStatusBadge status={lot.status} />
            {needsReviewHighlight && <Badge tone="orange">Неполные данные</Badge>}
            <span className="text-sm text-slate-500">
              {formatMoney(lot.budgetAmount, lot.currency)}
            </span>
          </div>
        </div>
        <Link to={`/admin/lots/${lot.id}/edit`}>
          <Button variant="secondary">Редактировать</Button>
        </Link>
      </div>

      <section className="grid gap-4 rounded-xl border border-slate-200 bg-white p-6 sm:grid-cols-2 lg:grid-cols-3">
        <Detail label="Категория" value={lot.category} />
        <Detail label="Количество" value={lot.quantity != null ? `${lot.quantity} ${lot.unit ?? ""}` : "—"} />
        <Detail label="Источник" value={lot.source ?? "—"} />
        <Detail label="Место поставки" value={lot.deliveryLocation ?? "—"} />
        <Detail label="Дедлайн подачи" value={lot.submissionDeadline?.slice(0, 10) ?? "—"} />
        <Detail label="Matches" value={String(lot.matchCount)} />
        <Detail label="Создан" value={lot.createdAt.slice(0, 10)} />
        <Detail label="Обновлён" value={lot.updatedAt.slice(0, 10)} />
        {lot.missingKeyFields.length > 0 && (
          <div className="sm:col-span-2 lg:col-span-3">
            <p className="text-xs font-semibold uppercase text-slate-400">Не заполнено</p>
            <p className="mt-1 text-sm text-amber-700">{lot.missingKeyFields.join(", ")}</p>
          </div>
        )}
        {lot.description && (
          <div className="sm:col-span-2 lg:col-span-3">
            <p className="text-xs font-semibold uppercase text-slate-400">Описание</p>
            <p className="mt-1 text-sm text-slate-700">{lot.description}</p>
          </div>
        )}
        {lot.technicalRequirements && (
          <div className="sm:col-span-2 lg:col-span-3">
            <p className="text-xs font-semibold uppercase text-slate-400">Тех. требования</p>
            <p className="mt-1 text-sm text-slate-700">{lot.technicalRequirements}</p>
          </div>
        )}
      </section>

      <div className="rounded-xl border border-slate-200 bg-white p-6">
        <LotAttachmentsSection lotId={lot.id} readOnly />
      </div>

      <PotentialMatchesPanel
        lotId={lot.id}
        matches={potentialMatches}
        loading={potentialLoading}
        error={potentialError}
        onSent={handleSent}
      />

      <section className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Созданные matches</h2>
        {matchesLoading && <p className="text-sm text-slate-500">Загрузка…</p>}
        {!matchesLoading && matches.length === 0 && (
          <p className="text-sm text-slate-500">Matches ещё не созданы</p>
        )}
        {matches.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b text-xs uppercase text-slate-500">
                  <th className="py-2 pr-4">Статус</th>
                  <th className="py-2 pr-4">Offer ID</th>
                  <th className="py-2 pr-4">Причина</th>
                  <th className="py-2 pr-4">Отправлен</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {matches.map((m) => (
                  <tr key={m.id}>
                    <td className="py-2 pr-4">
                      <Badge tone={m.status === "matched" ? "green" : "orange"}>
                        {m.status}
                      </Badge>
                    </td>
                    <td className="py-2 pr-4 font-mono text-xs text-slate-600">
                      {m.offerId.slice(0, 8)}…
                    </td>
                    <td className="py-2 pr-4 text-slate-600">{m.matchReason ?? "—"}</td>
                    <td className="py-2 pr-4 text-slate-600">
                      {m.sentToDistributorAt
                        ? m.sentToDistributorAt.slice(0, 16).replace("T", " ")
                        : "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase text-slate-400">{label}</p>
      <p className="mt-1 text-sm text-slate-800">{value}</p>
    </div>
  );
}
