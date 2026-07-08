import { Loader2, Upload } from "lucide-react";
import { useRef, useState } from "react";
import { uploadRawFile } from "../../../../api/imports";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import { Button } from "../../../../components/ui/Button";
import { ImportRunsTable } from "../components/ImportRunsTable";
import { useDistributorPermissions } from "../../hooks/useDistributorPermissions";
import { useImportRuns } from "../hooks/useImportRuns";

export function ImportRunsListPage() {
  const { token, user } = useAuth();
  const { canReadImports, canRunImport } = useDistributorPermissions();
  const { items, page, totalPages, loading, error, setPage, reload } = useImportRuns();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const [uploadSuccess, setUploadSuccess] = useState("");

  if (!canReadImports) {
    return (
      <p className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800">
        У вас нет доступа к импортам.
      </p>
    );
  }

  async function handleUpload(file: File) {
    if (!token || !user?.organizationId) return;
    setUploading(true);
    setUploadError("");
    setUploadSuccess("");
    try {
      await uploadRawFile(user.organizationId, file, token);
      setUploadSuccess(`Файл «${file.name}» загружен. Обработка начнётся автоматически.`);
      await reload();
    } catch (err) {
      setUploadError(err instanceof ApiError ? err.message : "Не удалось загрузить файл");
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Импорты прайс-листа</h1>
        <p className="mt-1 text-sm text-slate-500">
          Загрузка CSV/XLSX и контроль обработки строк перед применением к офферам
        </p>
      </div>

      {canRunImport && (
        <div className="rounded-xl border border-dashed border-brand-200 bg-brand-50/40 p-6">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="font-medium text-slate-900">Загрузить новый файл</p>
              <p className="mt-1 text-sm text-slate-600">CSV или XLSX, до 50 МБ</p>
            </div>
            <div className="flex items-center gap-3">
              <input
                ref={fileInputRef}
                type="file"
                accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                className="hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) handleUpload(file);
                }}
              />
              <Button
                loading={uploading}
                onClick={() => fileInputRef.current?.click()}
              >
                <Upload className="h-4 w-4" />
                Выбрать файл
              </Button>
            </div>
          </div>
          {uploadError && (
            <p className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{uploadError}</p>
          )}
          {uploadSuccess && (
            <p className="mt-3 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              {uploadSuccess}
            </p>
          )}
        </div>
      )}

      {loading && (
        <p className="flex items-center justify-center gap-2 text-sm text-slate-500">
          <Loader2 className="h-4 w-4 animate-spin" />
          Загрузка…
        </p>
      )}
      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}
      {!loading && !error && items.length === 0 && (
        <p className="rounded-xl border border-dashed border-slate-200 bg-white py-16 text-center text-sm text-slate-500">
          Импортов пока нет. Загрузите первый файл прайс-листа.
        </p>
      )}
      {!loading && !error && items.length > 0 && <ImportRunsTable items={items} />}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <Button
            variant="secondary"
            disabled={page <= 0 || loading}
            onClick={() => setPage(page - 1)}
          >
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
