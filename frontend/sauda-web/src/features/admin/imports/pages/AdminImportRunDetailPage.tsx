import { ArrowLeft, Loader2 } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { ImportStatusBadge } from "../../../distributor/imports/components/ImportStatusBadge";
import { ParsedRowsTable } from "../../../distributor/imports/components/ParsedRowsTable";
import { useAdminPermissions } from "../../hooks/useAdminPermissions";
import { useAdminImportRun } from "../hooks/useAdminImportRun";
import { formatDateTime } from "../../../../utils/format";

export function AdminImportRunDetailPage() {
  const { distributorId, runId } = useParams<{ distributorId: string; runId: string }>();
  const { canReadImports } = useAdminPermissions();
  const {
    run,
    rows,
    loading,
    rowsLoading,
    error,
    rowsError,
    statusFilter,
    setStatusFilter,
    isProcessing,
  } = useAdminImportRun(distributorId, runId);

  if (!canReadImports) {
    return (
      <p className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800">
        Раздел импортов доступен только администратору платформы.
      </p>
    );
  }

  return (
    <div className="space-y-6">
      <Link
        to="/admin/imports"
        className="inline-flex items-center gap-1 text-sm font-medium text-brand-600 hover:text-brand-700"
      >
        <ArrowLeft className="h-4 w-4" />
        К списку импортов
      </Link>

      {loading && (
        <p className="flex items-center gap-2 text-sm text-slate-500">
          <Loader2 className="h-4 w-4 animate-spin" />
          Загрузка…
        </p>
      )}
      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}

      {run && (
        <>
          <div className="rounded-xl border border-slate-200 bg-white p-6">
            <div className="mb-2 inline-flex rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
              Read-only
            </div>
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <h1 className="text-xl font-bold text-slate-900">{run.originalFilename}</h1>
                <p className="mt-1 text-sm text-slate-500">
                  {run.distributorName ?? "Дистрибьютор"} · создан {formatDateTime(run.createdAt)}
                </p>
              </div>
              <ImportStatusBadge status={run.status} />
            </div>

            {isProcessing && (
              <div className="mt-4 flex items-center gap-2 rounded-lg bg-blue-50 px-3 py-2 text-sm text-blue-800">
                <Loader2 className="h-4 w-4 animate-spin" />
                Обработка файла…
              </div>
            )}

            <dl className="mt-4 grid gap-4 sm:grid-cols-4">
              <div>
                <dt className="text-xs font-medium uppercase text-slate-500">Всего строк</dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900">{run.totalRows}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase text-slate-500">Разобрано</dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900">{run.parsedRowsCount}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase text-slate-500">Ошибок</dt>
                <dd className="mt-1 text-lg font-semibold text-red-600">{run.errorRowsCount}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase text-slate-500">Distributor ID</dt>
                <dd className="mt-1 font-mono text-xs text-slate-700">{run.distributorId}</dd>
              </div>
            </dl>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-slate-900">Черновые строки</h2>
            {rowsLoading && !isProcessing && (
              <p className="mt-2 text-sm text-slate-500">Загрузка строк…</p>
            )}
            {rowsError && (
              <p className="mt-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{rowsError}</p>
            )}
            {!rowsLoading && !rowsError && rows.length === 0 && (
              <p className="mt-2 rounded-lg border border-dashed border-slate-200 bg-white px-4 py-8 text-center text-sm text-slate-500">
                {isProcessing ? "Строки появятся после обработки" : "Строки не найдены"}
              </p>
            )}
            {rows.length > 0 && (
              <div className="mt-4">
                <ParsedRowsTable
                  items={rows}
                  readOnly
                  statusFilter={statusFilter}
                  onStatusFilterChange={setStatusFilter}
                />
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
