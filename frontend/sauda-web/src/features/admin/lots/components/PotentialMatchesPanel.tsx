import { useState } from "react";
import type { PotentialMatch } from "../types";
import { formatMoney, formatPercent } from "../../../../utils/format";
import { Badge, MatchScoreBadge } from "../../../../components/ui/Badge";
import { Button } from "../../../../components/ui/Button";
import { SendToDistributorModal } from "./SendToDistributorModal";

interface PotentialMatchesPanelProps {
  lotId: string;
  matches: PotentialMatch[];
  loading: boolean;
  error: string;
  onSent: () => void;
}

export function PotentialMatchesPanel({
  lotId,
  matches,
  loading,
  error,
  onSent,
}: PotentialMatchesPanelProps) {
  const [selected, setSelected] = useState<PotentialMatch | null>(null);

  return (
    <section className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Потенциальные совпадения</h2>
        <span className="text-sm text-slate-500">{matches.length} предложений</span>
      </div>

      {loading && <p className="text-sm text-slate-500">Загрузка…</p>}
      {error && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
      )}

      {!loading && !error && matches.length === 0 && (
        <p className="text-sm text-slate-500">Подходящих предложений не найдено</p>
      )}

      {matches.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[800px] text-left text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-xs font-semibold uppercase text-slate-500">
                <th className="py-2 pr-4">Score</th>
                <th className="py-2 pr-4">Offer</th>
                <th className="py-2 pr-4">Дистрибьютор</th>
                <th className="py-2 pr-4">Цена</th>
                <th className="py-2 pr-4">Остаток</th>
                <th className="py-2 pr-4">Причина</th>
                <th className="py-2">Действие</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {matches.map((m) => (
                <tr key={m.offerId}>
                  <td className="py-3 pr-4">
                    <MatchScoreBadge percent={formatPercent(m.confidenceScore)} />
                  </td>
                  <td className="py-3 pr-4 font-medium text-slate-900">{m.offerName}</td>
                  <td className="py-3 pr-4 text-slate-600">{m.distributorName}</td>
                  <td className="py-3 pr-4">{formatMoney(m.price)}</td>
                  <td className="py-3 pr-4 text-slate-600">{m.stockQuantity ?? "—"}</td>
                  <td className="max-w-[200px] py-3 pr-4 text-xs text-slate-500">
                    {m.matchReason}
                    {m.missingData.length > 0 && (
                      <span className="mt-1 block text-amber-600">
                        {m.missingData.join("; ")}
                      </span>
                    )}
                  </td>
                  <td className="py-3">
                    <div className="flex items-center gap-2">
                      <Badge tone={m.recommendedStatus === "matched" ? "green" : "orange"}>
                        {m.recommendedStatus}
                      </Badge>
                      <Button
                        variant="primary"
                        className="px-3 py-1.5 text-xs"
                        onClick={() => setSelected(m)}
                      >
                        Отправить
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <SendToDistributorModal
        lotId={lotId}
        match={selected}
        onClose={() => setSelected(null)}
        onSuccess={() => {
          setSelected(null);
          onSent();
        }}
      />
    </section>
  );
}
