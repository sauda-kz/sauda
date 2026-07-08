import { Link } from "react-router-dom";
import { Archive, Edit, Eye } from "lucide-react";
import type { Lot } from "../types";
import { formatDeadline, formatMoney, isDeadlineUrgent } from "../../../../utils/format";
import { Button } from "../../../../components/ui/Button";
import { LotStatusBadge } from "./LotStatusBadge";

interface LotsTableProps {
  items: Lot[];
  totalElements: number;
  onArchive: (lot: Lot) => void;
  archivingId?: string | null;
}

export function LotsTable({
  items,
  totalElements,
  onArchive,
  archivingId,
}: LotsTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[960px] text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 bg-slate-50/80 text-xs font-semibold uppercase tracking-wide text-slate-500">
              <th className="px-4 py-3">Название</th>
              <th className="px-4 py-3">Заказчик</th>
              <th className="px-4 py-3">Категория</th>
              <th className="px-4 py-3">Бюджет</th>
              <th className="px-4 py-3">Дедлайн</th>
              <th className="px-4 py-3">Статус</th>
              <th className="px-4 py-3">Matches</th>
              <th className="px-4 py-3">Действия</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {items.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-12 text-center text-slate-500">
                  Лотов пока нет
                </td>
              </tr>
            ) : (
              items.map((lot) => {
                const urgent = isDeadlineUrgent(lot.submissionDeadline);
                return (
                  <tr key={lot.id} className="hover:bg-slate-50/50">
                    <td className="max-w-[200px] px-4 py-4 font-medium text-slate-900">
                      {lot.title}
                    </td>
                    <td className="px-4 py-4 text-slate-600">{lot.customerName}</td>
                    <td className="px-4 py-4 text-slate-600">{lot.category}</td>
                    <td className="whitespace-nowrap px-4 py-4">
                      {formatMoney(lot.budgetAmount, lot.currency)}
                    </td>
                    <td
                      className={`whitespace-nowrap px-4 py-4 ${urgent ? "font-medium text-red-600" : "text-slate-600"}`}
                    >
                      {formatDeadline(lot.submissionDeadline)}
                    </td>
                    <td className="px-4 py-4">
                      <LotStatusBadge status={lot.status} />
                    </td>
                    <td className="px-4 py-4 text-slate-600">{lot.matchCount}</td>
                    <td className="px-4 py-4">
                      <div className="flex items-center gap-1">
                        <Link to={`/admin/lots/${lot.id}`}>
                          <Button variant="ghost" className="px-2 py-1.5" title="Открыть">
                            <Eye className="h-4 w-4" />
                          </Button>
                        </Link>
                        <Link to={`/admin/lots/${lot.id}/edit`}>
                          <Button variant="ghost" className="px-2 py-1.5" title="Редактировать">
                            <Edit className="h-4 w-4" />
                          </Button>
                        </Link>
                        {lot.status !== "archived" && (
                          <Button
                            variant="ghost"
                            className="px-2 py-1.5 text-red-600"
                            title="Архивировать"
                            loading={archivingId === lot.id}
                            onClick={() => onArchive(lot)}
                          >
                            <Archive className="h-4 w-4" />
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
      <div className="border-t border-slate-100 px-6 py-3 text-sm text-slate-500">
        Показано {items.length} из {totalElements}
      </div>
    </div>
  );
}
