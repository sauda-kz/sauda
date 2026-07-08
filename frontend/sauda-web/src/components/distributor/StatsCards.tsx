import { Layers, Package, Upload } from "lucide-react";

interface StatsCardsProps {
  totalMatches: number;
  uploadedProductsCount?: number;
  inStockCount?: number;
}

export function StatsCards({
  totalMatches,
  uploadedProductsCount = 0,
  inStockCount = 0,
}: StatsCardsProps) {
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              Загружено товаров
            </p>
            <p className="mt-2 text-3xl font-bold text-slate-900">
              {uploadedProductsCount.toLocaleString("ru-RU")}
            </p>
            <p className="mt-1 text-sm text-slate-500">позиций в прайсе</p>
          </div>
          <Upload className="h-5 w-5 text-slate-300" />
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              Товаров в наличии
            </p>
            <p className="mt-2 text-3xl font-bold text-slate-900">
              {inStockCount.toLocaleString("ru-RU")}
            </p>
            <p className="mt-1 text-sm text-slate-500">с ненулевым остатком</p>
          </div>
          <Package className="h-5 w-5 text-slate-300" />
        </div>
      </div>

      <div className="rounded-xl border border-brand-600 bg-brand-600 p-5 text-white shadow-sm">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wide text-brand-100">
              Подходящих лотов
            </p>
            <p className="mt-2 text-3xl font-bold">
              {totalMatches.toLocaleString("ru-RU")}
            </p>
            <p className="mt-1 text-sm text-brand-100">активных совпадений</p>
          </div>
          <Layers className="h-5 w-5 text-brand-200" />
        </div>
      </div>
    </div>
  );
}
