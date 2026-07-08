import { Building2, MapPin, Upload } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "../ui/Button";
import type { OrganizationResponse } from "../../types/api";
import { formatPriceListUpdatedAt } from "../../utils/format";

interface CompanyBannerProps {
  organization: OrganizationResponse;
  lastPriceListUpdatedAt?: string | null;
}

export function CompanyBanner({ organization, lastPriceListUpdatedAt = null }: CompanyBannerProps) {
  const priceListUpdatedLabel = formatPriceListUpdatedAt(lastPriceListUpdatedAt);
  const hasPriceList = priceListUpdatedLabel != null;

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex gap-4">
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-400">
            <Building2 className="h-7 w-7" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900">{organization.name}</h1>
            <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-slate-500">
              <span className="inline-flex items-center gap-1">
                <MapPin className="h-4 w-4" />
                г. Алматы
              </span>
              <span className="rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700">
                Активная компания
              </span>
            </div>
            <p className="mt-3 text-xs font-medium uppercase tracking-wide text-slate-400">
              Последнее обновление прайса
            </p>
            <p className="text-sm text-slate-600">
              {hasPriceList ? priceListUpdatedLabel : "Прайс ещё не загружался"}
            </p>
            {hasPriceList ? (
              <span className="mt-2 inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                Актуальные
              </span>
            ) : (
              <span className="mt-2 inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
                <span className="h-1.5 w-1.5 rounded-full bg-slate-400" />
                Нет данных
              </span>
            )}
          </div>
        </div>

        <Link to="#" className="shrink-0">
          <Button className="w-full sm:w-auto">
            <Upload className="h-4 w-4" />
            Загрузить / обновить прайс
          </Button>
        </Link>
      </div>
    </div>
  );
}
