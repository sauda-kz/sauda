import { Link } from "react-router-dom";
import { AlertTriangle } from "lucide-react";
import type { DistributorLotMatchCard } from "../../../../types/api";
import {
  formatDeadline,
  formatMoney,
  formatPercent,
  formatQuantity,
  formatStockStatus,
  isDeadlineUrgent,
  prettifyCode,
} from "../../../../utils/format";
import { Badge, MatchScoreBadge } from "../../../../components/ui/Badge";

interface SuitableLotCardProps {
  match: DistributorLotMatchCard;
}

const statusLabels: Record<string, string> = {
  matched: "Новый",
  needs_review: "На проверке",
  interested: "Интересно",
  dismissed: "Скрыт",
  mismatch_reported: "Несоответствие",
};

export function SuitableLotCard({ match }: SuitableLotCardProps) {
  const pct = formatPercent(match.confidenceScore);
  const urgent = isDeadlineUrgent(match.submissionDeadline);

  return (
    <Link
      to={`/suitable-lots/${match.matchId}`}
      className="block rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md"
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <MatchScoreBadge percent={pct} />
            <Badge tone="blue">{statusLabels[match.status] ?? match.status}</Badge>
            {match.category && <Badge tone="gray">{match.category}</Badge>}
          </div>
          <h2 className="mt-2 text-lg font-semibold text-slate-900">{match.title}</h2>
          <p className="mt-1 text-sm text-slate-600">{match.customerName}</p>
        </div>
        <div className="text-right">
          <p className="text-lg font-bold text-brand-600">
            {formatMoney(match.budgetAmount, match.currency)}
          </p>
          <p className={`text-sm ${urgent ? "font-medium text-red-600" : "text-slate-500"}`}>
            {formatDeadline(match.submissionDeadline)}
          </p>
        </div>
      </div>

      <div className="mt-4 grid gap-3 border-t border-slate-100 pt-4 sm:grid-cols-2">
        <div>
          <p className="text-xs font-semibold uppercase text-slate-400">Товар</p>
          <p className="mt-1 text-sm font-medium text-slate-900">{match.offerName}</p>
          {match.brand && (
            <p className="text-xs text-slate-500">
              {match.brand} {match.modelMpn}
            </p>
          )}
        </div>
        <div>
          <p className="text-xs font-semibold uppercase text-slate-400">Цена / остаток</p>
          <p className="mt-1 text-sm text-slate-900">
            {formatMoney(match.estimatedUnitPrice, match.currency)} ·{" "}
            {formatQuantity(match.availableQuantity, "шт")}
          </p>
          {match.stockStatus && (
            <p className="text-xs text-slate-500">
              Наличие: {formatStockStatus(match.stockStatus)}
            </p>
          )}
        </div>
      </div>

      {match.riskFlags.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-2">
          {match.riskFlags.map((risk) => (
            <span
              key={risk}
              className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-xs text-amber-800"
            >
              <AlertTriangle className="h-3 w-3" />
              {prettifyCode(risk)}
            </span>
          ))}
        </div>
      )}
    </Link>
  );
}
