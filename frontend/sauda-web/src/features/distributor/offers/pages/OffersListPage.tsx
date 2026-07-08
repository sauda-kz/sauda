import { Loader2, Search } from "lucide-react";
import { Button } from "../../../../components/ui/Button";
import { OffersTable } from "../components/OffersTable";
import { useDistributorPermissions } from "../../hooks/useDistributorPermissions";
import { useOffers } from "../hooks/useOffers";
import type { StockStatus } from "../types";

const stockOptions: { value: StockStatus | ""; label: string }[] = [
  { value: "", label: "Все остатки" },
  { value: "in_stock", label: "В наличии" },
  { value: "low_stock", label: "Мало" },
  { value: "out_of_stock", label: "Нет в наличии" },
  { value: "on_order", label: "Под заказ" },
  { value: "unknown", label: "Не указано" },
];

export function OffersListPage() {
  const { canReadOffers } = useDistributorPermissions();
  const {
    items,
    total,
    page,
    totalPages,
    q,
    stockStatus,
    loading,
    error,
    setPage,
    setQ,
    setStockStatus,
  } = useOffers();

  if (!canReadOffers) {
    return (
      <p className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800">
        У вас нет доступа к прайс-листу.
      </p>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Мой прайс</h1>
        <p className="mt-1 text-sm text-slate-500">
          Актуальные позиции вашей компании после импорта прайс-листа
          {total > 0 ? ` · всего ${total.toLocaleString("ru-RU")} позиций` : ""}
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            value={q}
            onChange={(event) => setQ(event.target.value)}
            placeholder="Поиск по наименованию, бренду, модели…"
            className="w-full rounded-lg border border-slate-200 py-2.5 pl-9 pr-3 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
          />
        </div>
        <select
          value={stockStatus}
          onChange={(event) => setStockStatus(event.target.value as StockStatus | "")}
          className="rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
        >
          {stockOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {loading && (
        <p className="flex items-center justify-center gap-2 text-sm text-slate-500">
          <Loader2 className="h-4 w-4 animate-spin" />
          Загрузка…
        </p>
      )}
      {error && <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
      {!loading && !error && items.length === 0 && (
        <p className="rounded-xl border border-dashed border-slate-200 bg-white py-16 text-center text-sm text-slate-500">
          Позиций пока нет. Загрузите прайс-лист в разделе «Импорты».
        </p>
      )}
      {!loading && !error && items.length > 0 && <OffersTable items={items} />}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <Button variant="secondary" disabled={page <= 0 || loading} onClick={() => setPage(page - 1)}>
            Назад
          </Button>
          <span className="text-sm text-slate-600">
            Страница {page + 1} из {totalPages}
          </span>
          <Button
            variant="secondary"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => setPage(page + 1)}
          >
            Вперёд
          </Button>
        </div>
      )}
    </div>
  );
}
