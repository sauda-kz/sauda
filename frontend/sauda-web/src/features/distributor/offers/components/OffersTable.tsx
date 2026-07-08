import { OfferStockBadge } from "./OfferStockBadge";
import { formatMoney } from "../../../../utils/format";
import type { Offer } from "../types";

interface OffersTableProps {
  items: Offer[];
}

function vatLabel(priceIncludesVat: boolean | null): string {
  if (priceIncludesVat === true) return "с НДС";
  if (priceIncludesVat === false) return "без НДС";
  return "НДС не указан";
}

export function OffersTable({ items }: OffersTableProps) {
  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
          <tr>
            <th className="px-4 py-3">Наименование</th>
            <th className="px-4 py-3">Бренд / модель</th>
            <th className="px-4 py-3">Категория</th>
            <th className="px-4 py-3 text-right">Цена</th>
            <th className="px-4 py-3 text-right">Остаток</th>
            <th className="px-4 py-3">Наличие</th>
            <th className="px-4 py-3">Срок</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {items.map((offer) => (
            <tr key={offer.id} className="hover:bg-slate-50">
              <td className="px-4 py-3 font-medium text-slate-900">{offer.rawName}</td>
              <td className="px-4 py-3 text-slate-600">
                {[offer.brand, offer.modelMpn].filter(Boolean).join(" · ") || "—"}
              </td>
              <td className="px-4 py-3 text-slate-600">{offer.category ?? "—"}</td>
              <td className="px-4 py-3 text-right">
                <div className="font-medium text-slate-900">
                  {formatMoney(offer.price, offer.currency)}
                </div>
                <div className="text-xs text-slate-400">{vatLabel(offer.priceIncludesVat)}</div>
              </td>
              <td className="px-4 py-3 text-right text-slate-700">
                {offer.stockQuantity ?? "—"}
              </td>
              <td className="px-4 py-3">
                <OfferStockBadge status={offer.stockStatus} />
              </td>
              <td className="px-4 py-3 text-slate-600">{offer.leadTime ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
