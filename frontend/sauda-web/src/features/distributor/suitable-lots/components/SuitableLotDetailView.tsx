import { Link } from "react-router-dom";
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  ExternalLink,
} from "lucide-react";
import type { DistributorLotMatchCard } from "../../../../types/api";
import {
  formatDeadline,
  formatMoney,
  formatPercent,
  formatQuantity,
  prettifyCode,
} from "../../../../utils/format";
import { Badge } from "../../../../components/ui/Badge";
import { LotMatchStatusActions } from "./LotMatchStatusActions";

interface SuitableLotDetailViewProps {
  match: DistributorLotMatchCard;
  canManage: boolean;
  actionLoading: boolean;
  onStatusChange: (status: import("../../../../types/api").LotMatchStatus) => void;
}

export function SuitableLotDetailView({
  match,
  canManage,
  actionLoading,
  onStatusChange,
}: SuitableLotDetailViewProps) {
  const pct = formatPercent(match.confidenceScore);

  return (
    <div>
      <Link
        to="/suitable-lots"
        className="mb-6 inline-flex items-center gap-2 text-sm text-slate-500 hover:text-brand-600"
      >
        <ArrowLeft className="h-4 w-4" />
        Назад к подходящим лотам
      </Link>

      <div className="grid gap-6 lg:grid-cols-5">
        <div className="lg:col-span-3 space-y-4">
          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">
              Закупочный лот
            </p>
            <h1 className="mt-2 text-2xl font-bold text-slate-900">{match.title}</h1>
            <div className="mt-4 flex flex-wrap gap-2">
              {match.category && <Badge tone="blue">{match.category}</Badge>}
              <Badge tone="gray">{formatMoney(match.budgetAmount, match.currency)}</Badge>
            </div>
            <dl className="mt-6 grid gap-4 sm:grid-cols-2">
              <Detail label="Заказчик" value={match.customerName} />
              <Detail label="Дедлайн" value={formatDeadline(match.submissionDeadline)} />
              <Detail label="Место поставки" value={match.deliveryLocation ?? "—"} />
              <Detail
                label="Количество"
                value={formatQuantity(match.quantity, match.unit)}
              />
            </dl>
            {match.matchReason && (
              <div className="mt-6">
                <h3 className="text-sm font-semibold text-slate-900">Причина совпадения</h3>
                <p className="mt-2 text-sm text-slate-600">{match.matchReason}</p>
              </div>
            )}
            {match.sourceUrl && (
              <a
                href={match.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-4 inline-flex items-center gap-2 text-sm font-medium text-brand-600"
              >
                Ссылка на закупку
                <ExternalLink className="h-4 w-4" />
              </a>
            )}
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <LotMatchStatusActions
              canManage={canManage}
              loading={actionLoading}
              onStatusChange={onStatusChange}
            />
          </div>
        </div>

        <div className="space-y-4 lg:col-span-2">
          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase text-slate-400">Ваш товар</p>
            <h2 className="mt-2 text-lg font-bold text-slate-900">{match.offerName}</h2>
            <dl className="mt-4 space-y-2 text-sm">
              <Row label="Бренд" value={match.brand} />
              <Row label="Модель" value={match.modelMpn} />
              <Row
                label="Цена"
                value={formatMoney(match.estimatedUnitPrice, match.currency)}
              />
              <Row
                label="Остаток"
                value={formatQuantity(match.availableQuantity, "шт")}
              />
            </dl>
            {pct != null && (
              <div className="mt-4">
                <Badge tone="green">{pct}% совпадение</Badge>
              </div>
            )}
          </div>

          {match.matchedRequirements.length > 0 && (
            <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
              <p className="text-xs font-semibold uppercase text-slate-400">Совпало</p>
              <div className="mt-3 flex flex-wrap gap-2">
                {match.matchedRequirements.map((req) => (
                  <span
                    key={req}
                    className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    {prettifyCode(req)}
                  </span>
                ))}
              </div>
            </div>
          )}

          {match.riskFlags.length > 0 && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-5">
              <p className="text-xs font-semibold uppercase text-amber-800">Риски</p>
              <div className="mt-3 flex flex-wrap gap-2">
                {match.riskFlags.map((risk) => (
                  <span
                    key={risk}
                    className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-800"
                  >
                    <AlertTriangle className="h-3.5 w-3.5" />
                    {prettifyCode(risk)}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase text-slate-400">{label}</dt>
      <dd className="mt-1 text-sm text-slate-900">{value}</dd>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right text-slate-900">{value ?? "—"}</dd>
    </div>
  );
}
