import { Bookmark, EyeOff, Info } from "lucide-react";
import { Link } from "react-router-dom";
import type { DistributorLotMatchCard } from "../../types/api";
import {
  formatDeadline,
  formatMarginPercent,
  formatMoney,
  formatPercent,
  formatQuantity,
  isDeadlineUrgent,
} from "../../utils/format";
import { Button } from "../ui/Button";
import { MatchScoreBadge } from "../ui/Badge";

interface LotsTableProps {
  items: DistributorLotMatchCard[];
  totalElements: number;
  pageSize: number;
}

export function LotsTable({ items, totalElements, pageSize }: LotsTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
        <h2 className="text-lg font-semibold text-slate-900">Подходящие лоты</h2>
        <select
          className="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-600"
          defaultValue="all"
          aria-label="Фильтр"
        >
          <option value="all">Все совпадения</option>
        </select>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[900px] text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50/80 text-xs font-semibold uppercase tracking-wide text-slate-500">
              <th className="px-4 py-3">Совпадение</th>
              <th className="px-4 py-3">Лот</th>
              <th className="px-4 py-3">Заказчик</th>
              <th className="px-4 py-3">Сумма</th>
              <th className="px-4 py-3">Дедлайн</th>
              <th className="px-4 py-3">Найденный товар</th>
              <th className="px-4 py-3">Остаток</th>
              <th className="px-4 py-3">Маржа</th>
              <th className="px-4 py-3">Действия</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {items.length === 0 ? (
              <tr>
                <td colSpan={9} className="px-4 py-12 text-center text-slate-500">
                  Подходящих лотов пока нет
                </td>
              </tr>
            ) : (
              items.map((item) => {
                const pct = formatPercent(item.confidenceScore);
                const marginPct = formatMarginPercent(item.estimatedMargin, item.budgetAmount);
                const urgent = isDeadlineUrgent(item.submissionDeadline);

                return (
                  <tr key={item.matchId} className="hover:bg-slate-50/50">
                    <td className="px-4 py-4">
                      <MatchScoreBadge percent={pct} />
                    </td>
                    <td className="max-w-[180px] px-4 py-4 font-medium text-slate-900">
                      {item.title}
                    </td>
                    <td className="px-4 py-4 text-slate-600">{item.customerName}</td>
                    <td className="whitespace-nowrap px-4 py-4 text-slate-900">
                      {formatMoney(item.budgetAmount, item.currency)}
                    </td>
                    <td
                      className={`whitespace-nowrap px-4 py-4 ${urgent ? "font-medium text-red-600" : "text-slate-600"}`}
                    >
                      {formatDeadline(item.submissionDeadline)}
                    </td>
                    <td className="max-w-[200px] px-4 py-4">
                      <p className="font-medium text-slate-900">{item.offerName}</p>
                      {item.modelMpn && (
                        <p className="text-xs text-slate-400">{item.modelMpn}</p>
                      )}
                    </td>
                    <td className="px-4 py-4 text-slate-600">
                      {formatQuantity(item.availableQuantity, "шт")}
                    </td>
                    <td className="px-4 py-4 text-slate-600">
                      <span className="inline-flex items-center gap-1">
                        {marginPct ?? "—"}
                        {marginPct && <Info className="h-3.5 w-3.5 text-slate-300" />}
                      </span>
                    </td>
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-1">
                        <Link to={`/lots/${item.matchId}`}>
                          <Button variant="primary" className="px-3 py-1.5 text-xs">
                            Подробнее
                          </Button>
                        </Link>
                        <button
                          type="button"
                          className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-brand-600"
                          aria-label="В закладки"
                        >
                          <Bookmark className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
                          aria-label="Скрыть"
                        >
                          <EyeOff className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between border-t border-slate-100 px-6 py-4 text-sm text-slate-500">
        <span>
          Показано {items.length} из {totalElements} лотов
        </span>
        {totalElements > pageSize && (
          <span className="font-medium text-brand-600">Смотреть все →</span>
        )}
      </div>
    </div>
  );
}
