import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Clock,
  ExternalLink,
  EyeOff,
  Star,
} from "lucide-react";
import { Link } from "react-router-dom";
import type { DistributorLotMatchCard, LotMatchStatus } from "../../types/api";
import {
  formatDeadline,
  formatMoney,
  formatPercent,
  formatQuantity,
} from "../../utils/format";
import { Badge } from "../ui/Badge";
import { Button } from "../ui/Button";

interface LotDetailViewProps {
  match: DistributorLotMatchCard;
  onStatusChange: (status: LotMatchStatus) => void;
  actionLoading: boolean;
}

export function LotDetailView({ match, onStatusChange, actionLoading }: LotDetailViewProps) {
  const pct = formatPercent(match.confidenceScore);

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <Link
        to="/lots"
        className="mb-6 inline-flex items-center gap-2 text-sm text-slate-500 hover:text-brand-600"
      >
        <ArrowLeft className="h-4 w-4" />
        Назад к лотам
      </Link>

      <div className="grid gap-6 lg:grid-cols-5">
        {/* Левая колонка — лот */}
        <div className="lg:col-span-3">
          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <p className="text-xs font-semibold uppercase tracking-wide text-brand-600">
              Закупочный лот
            </p>
            <h1 className="mt-2 text-2xl font-bold text-slate-900">{match.title}</h1>

            <div className="mt-4 flex flex-wrap gap-2">
              {match.category && (
                <Badge tone="blue">{match.category}</Badge>
              )}
              {match.deliveryLocation && (
                <Badge tone="gray">{match.deliveryLocation}</Badge>
              )}
              <Badge tone="gray">{formatMoney(match.budgetAmount, match.currency)}</Badge>
              {match.quantity != null && (
                <Badge tone="gray">{formatQuantity(match.quantity, match.unit)}</Badge>
              )}
            </div>

            <dl className="mt-6 grid gap-4 sm:grid-cols-2">
              <DetailItem label="Заказчик" value={match.customerName} />
              <DetailItem label="Сумма" value={formatMoney(match.budgetAmount, match.currency)} highlight />
              <DetailItem
                label="Количество"
                value={formatQuantity(match.quantity, match.unit)}
              />
              <DetailItem label="Место поставки" value={match.deliveryLocation ?? "—"} />
              <DetailItem
                label="Срок поставки"
                value={formatDeadline(match.deliveryDeadline)}
              />
              <DetailItem
                label="Дедлайн"
                value={formatDeadline(match.submissionDeadline)}
              />
            </dl>

            {match.matchReason && (
              <div className="mt-6">
                <h3 className="text-sm font-semibold text-slate-900">Технические требования</h3>
                <p className="mt-2 text-sm leading-relaxed text-slate-600">{match.matchReason}</p>
              </div>
            )}

            {match.requiredDocuments && (
              <div className="mt-6">
                <h3 className="text-sm font-semibold text-slate-900">Требования к документам</h3>
                <ul className="mt-2 list-inside list-disc text-sm text-slate-600">
                  {match.requiredDocuments.split(/[,;\n]/).map((doc) => (
                    <li key={doc.trim()}>{doc.trim()}</li>
                  ))}
                </ul>
              </div>
            )}

            {match.sourceUrl && (
              <a
                href={match.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-6 inline-flex items-center gap-2 text-sm font-medium text-brand-600 hover:text-brand-700"
              >
                Ссылка на закупку
                <ExternalLink className="h-4 w-4" />
              </a>
            )}
          </div>

          {/* Панель действий */}
          <div className="mt-4 flex flex-wrap items-center gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <Button
              loading={actionLoading}
              onClick={() => onStatusChange("interested")}
            >
              <Star className="h-4 w-4" />
              Интересно
            </Button>
            <Button
              variant="secondary"
              loading={actionLoading}
              onClick={() => onStatusChange("needs_review")}
            >
              <Clock className="h-4 w-4" />
              Оставить на проверку
            </Button>
            <Button
              variant="ghost"
              loading={actionLoading}
              onClick={() => onStatusChange("dismissed")}
            >
              <EyeOff className="h-4 w-4" />
              Скрыть лот
            </Button>
            {match.sourceUrl && (
              <a
                href={match.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="ml-auto inline-flex items-center gap-2 text-sm text-brand-600 hover:text-brand-700"
              >
                Перейти к источнику
                <ExternalLink className="h-4 w-4" />
              </a>
            )}
          </div>
        </div>

        {/* Правая колонка — анализ */}
        <div className="space-y-4 lg:col-span-2">
          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              Найденный товар
            </p>
            <h2 className="mt-2 text-lg font-bold text-slate-900">{match.offerName}</h2>
            <dl className="mt-4 space-y-2 text-sm">
              <Row label="Бренд" value={match.brand} />
              <Row label="Модель" value={match.modelMpn} />
              <Row
                label="Остаток"
                value={formatQuantity(match.availableQuantity, "шт")}
                highlight
              />
              <Row
                label="Цена дистрибьютора"
                value={formatMoney(match.estimatedUnitPrice, match.currency)}
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
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                Почему найдено совпадение
              </p>
              <div className="mt-3 flex flex-wrap gap-2">
                {match.matchedRequirements.map((req) => (
                  <span
                    key={req}
                    className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    {req}
                  </span>
                ))}
              </div>
            </div>
          )}

          {match.riskFlags.length > 0 && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-amber-800">
                Возможные риски
              </p>
              <div className="mt-3 flex flex-wrap gap-2">
                {match.riskFlags.map((risk) => (
                  <span
                    key={risk}
                    className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-800"
                  >
                    <AlertTriangle className="h-3.5 w-3.5" />
                    {risk}
                  </span>
                ))}
              </div>
            </div>
          )}

          {match.missingRequirements.length > 0 && (
            <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
              <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                Что нужно проверить
              </p>
              <ul className="mt-3 space-y-2">
                {match.missingRequirements.map((item) => (
                  <li
                    key={item}
                    className="flex items-start gap-2 text-sm text-slate-600"
                  >
                    <span className="mt-0.5 h-4 w-4 shrink-0 rounded-full border-2 border-slate-300" />
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function DetailItem({
  label,
  value,
  highlight,
}: {
  label: string;
  value: string;
  highlight?: boolean;
}) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</dt>
      <dd className={`mt-1 text-sm ${highlight ? "font-semibold text-brand-600" : "text-slate-900"}`}>
        {value}
      </dd>
    </div>
  );
}

function Row({
  label,
  value,
  highlight,
}: {
  label: string;
  value: string | null | undefined;
  highlight?: boolean;
}) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-slate-500">{label}</dt>
      <dd className={`text-right ${highlight ? "font-semibold text-brand-600" : "text-slate-900"}`}>
        {value ?? "—"}
      </dd>
    </div>
  );
}
