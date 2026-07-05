import { Loader2 } from "lucide-react";
import { useState } from "react";
import { Button } from "../../../../components/ui/Button";
import { useAdminPermissions } from "../../hooks/useAdminPermissions";
import { AdminImportRunsTable } from "../components/AdminImportRunsTable";
import { useAdminImportRuns } from "../hooks/useAdminImportRuns";
import type { ImportStatus } from "../../../distributor/imports/types";

const IMPORT_STATUS_OPTIONS: ImportStatus[] = [
  "pending",
  "processing",
  "parsed",
  "parsed_with_errors",
  "awaiting_approval",
  "approved",
  "applied",
  "rejected",
  "failed",
];

export function AdminImportRunsListPage() {
  const { canReadImports } = useAdminPermissions();
  const [distributorId, setDistributorId] = useState("");
  const [status, setStatus] = useState<ImportStatus | "">("");
  const { items, page, totalPages, loading, error, setPage } = useAdminImportRuns({
    distributorId: distributorId.trim() || undefined,
    status,
  });

  if (!canReadImports) {
    return (
      <p className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800">
        Раздел импортов доступен только администратору платформы.
      </p>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Импорты дистрибьюторов</h1>
        <p className="mt-1 text-sm text-slate-500">
          Read-only обзор для поддержки и отладки
        </p>
      </div>

      <div className="flex flex-wrap gap-4 rounded-xl border border-slate-200 bg-white p-4">
        <label className="text-sm font-medium text-slate-700">
          Distributor ID
          <input
            className="ml-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-mono"
            value={distributorId}
            onChange={(event) => setDistributorId(event.target.value)}
            placeholder="UUID"
          />
        </label>
        <label className="text-sm font-medium text-slate-700">
          Статус
          <select
            className="ml-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm"
            value={status}
            onChange={(event) => setStatus(event.target.value as ImportStatus | "")}
          >
            <option value="">Все</option>
            {IMPORT_STATUS_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loading && (
        <p className="flex items-center gap-2 text-sm text-slate-500">
          <Loader2 className="h-4 w-4 animate-spin" />
          Загрузка…
        </p>
      )}
      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}
      {!loading && !error && items.length === 0 && (
        <p className="rounded-xl border border-dashed border-slate-200 bg-white py-16 text-center text-sm text-slate-500">
          Импорты не найдены
        </p>
      )}
      {!loading && !error && items.length > 0 && <AdminImportRunsTable items={items} />}

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
