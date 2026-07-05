import { ArrowLeft, Loader2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ImportDecisionBar } from "../components/ImportDecisionBar";
import { ImportStatusBadge } from "../components/ImportStatusBadge";
import { ParsedRowEditModal } from "../components/ParsedRowEditModal";
import { ParsedRowsTable } from "../components/ParsedRowsTable";
import { useDistributorPermissions } from "../../hooks/useDistributorPermissions";
import { useApproveImport } from "../hooks/useApproveImport";
import { useImportRun } from "../hooks/useImportRun";
import { useParsedRows } from "../hooks/useParsedRows";
import { useRejectImport } from "../hooks/useRejectImport";
import { useUpdateParsedRow } from "../hooks/useUpdateParsedRow";
import type { ParsedRowResponse, ParsedRowStatus } from "../types";
import { formatDateTime } from "../../../../utils/format";

export function ImportRunDetailPage() {
  const { runId } = useParams<{ runId: string }>();
  const { canReadImports, canApproveImport } = useDistributorPermissions();
  const [statusFilter, setStatusFilter] = useState<ParsedRowStatus | "">("");
  const [editingRow, setEditingRow] = useState<ParsedRowResponse | null>(null);

  const { run, loading, error, reload, isProcessing } = useImportRun(runId);
  const {
    items: rows,
    loading: rowsLoading,
    error: rowsError,
    reload: reloadRows,
  } = useParsedRows(runId, statusFilter);

  const refreshAll = () => {
    reload();
    reloadRows(true);
  };

  const {
    update,
    loading: updateLoading,
    error: updateError,
    clearError,
  } = useUpdateParsedRow(runId, refreshAll);

  const {
    approve,
    loading: approveLoading,
    error: approveError,
    clearError: clearApproveError,
  } = useApproveImport(runId, refreshAll);

  const {
    reject,
    loading: rejectLoading,
    error: rejectError,
    clearError: clearRejectError,
  } = useRejectImport(runId, refreshAll);

  useEffect(() => {
    if (!isProcessing) return;
    const intervalId = window.setInterval(() => reloadRows(true), 3000);
    return () => window.clearInterval(intervalId);
  }, [isProcessing, reloadRows]);

  if (!canReadImports) {
    return (
      <p className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800">
        У вас нет доступа к импортам.
      </p>
    );
  }

  async function handleSave(rowId: string, payload: Parameters<typeof update>[1]) {
    const updated = await update(rowId, payload);
    if (updated) {
      setEditingRow(null);
      clearError();
    }
  }

  async function handleApprove() {
    clearApproveError();
    const updated = await approve();
    if (updated) refreshAll();
  }

  async function handleReject(reason?: string) {
    clearRejectError();
    const updated = await reject(reason);
    if (updated) refreshAll();
  }

  return (
    <div className="space-y-6">
      <Link
        to="/imports"
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
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <h1 className="text-xl font-bold text-slate-900">{run.originalFilename}</h1>
                <p className="mt-1 text-sm text-slate-500">
                  Создан {formatDateTime(run.createdAt)}
                  {run.adapterKey && ` · ${run.adapterKey}`}
                </p>
              </div>
              <ImportStatusBadge status={run.status} />
            </div>

            {isProcessing && (
              <div className="mt-4 flex items-center gap-2 rounded-lg bg-blue-50 px-3 py-2 text-sm text-blue-800">
                <Loader2 className="h-4 w-4 animate-spin" />
                Обработка файла… страница обновляется автоматически
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
                <dt className="text-xs font-medium uppercase text-slate-500">Завершён</dt>
                <dd className="mt-1 text-sm text-slate-700">{formatDateTime(run.finishedAt)}</dd>
              </div>
            </dl>
          </div>

          <ImportDecisionBar
            run={run}
            canApprove={canApproveImport}
            approveLoading={approveLoading}
            rejectLoading={rejectLoading}
            approveError={approveError}
            rejectError={rejectError}
            onApprove={handleApprove}
            onReject={handleReject}
          />

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
                  canEdit={canApproveImport && !["applied", "rejected", "failed"].includes(run.status)}
                  statusFilter={statusFilter}
                  onStatusFilterChange={setStatusFilter}
                  onEdit={(row) => {
                    clearError();
                    setEditingRow(row);
                  }}
                />
              </div>
            )}
          </div>
        </>
      )}

      <ParsedRowEditModal
        row={editingRow}
        open={editingRow != null}
        loading={updateLoading}
        error={updateError}
        onClose={() => {
          setEditingRow(null);
          clearError();
        }}
        onSave={handleSave}
      />
    </div>
  );
}
