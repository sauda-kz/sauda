import { useState } from "react";
import { Link } from "react-router-dom";
import { Plus } from "lucide-react";
import { archiveLot } from "../../../../api/lots";
import { useAuth } from "../../../../auth/AuthProvider";
import { ApiError } from "../../../../api/client";
import { Button } from "../../../../components/ui/Button";
import { InputField } from "../../../../components/ui/Input";
import { TableSkeleton } from "../../../../components/ui/Skeleton";
import { LotsTable } from "../components/LotsTable";
import { useLots } from "../hooks/useLots";
import type { Lot, LotStatus } from "../types";

export function LotsListPage() {
  const { token } = useAuth();
  const [q, setQ] = useState("");
  const [status, setStatus] = useState<LotStatus | "">("");
  const [category, setCategory] = useState("");
  const [source, setSource] = useState("");
  const [archivingId, setArchivingId] = useState<string | null>(null);

  const { page, loading, error, reload } = useLots({
    q: q || undefined,
    status: status || undefined,
    category: category || undefined,
    source: source || undefined,
    page: 0,
    size: 20,
  });

  async function handleArchive(lot: Lot) {
    if (!token || !confirm(`Архивировать лот «${lot.title}»?`)) return;
    setArchivingId(lot.id);
    try {
      await archiveLot(lot.id, token);
      await reload();
    } catch (err) {
      alert(err instanceof ApiError ? err.message : "Ошибка архивации");
    } finally {
      setArchivingId(null);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Лоты</h1>
          <p className="mt-1 text-sm text-slate-500">Управление закупочными лотами</p>
        </div>
        <Link to="/admin/lots/new">
          <Button>
            <Plus className="h-4 w-4" />
            Создать лот
          </Button>
        </Link>
      </div>

      <div className="grid gap-3 rounded-xl border border-slate-200 bg-white p-4 sm:grid-cols-4">
        <InputField
          label="Поиск"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Название, заказчик…"
        />
        <label className="block text-sm font-medium text-slate-700">
          Статус
          <select
            className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 shadow-xs outline-none transition-colors hover:border-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/25"
            value={status}
            onChange={(e) => setStatus(e.target.value as LotStatus | "")}
          >
            <option value="">Все</option>
            <option value="active">Активный</option>
            <option value="needs_review">Требует проверки</option>
            <option value="draft">Черновик</option>
            <option value="archived">Архив</option>
          </select>
        </label>
        <InputField label="Категория" value={category} onChange={(e) => setCategory(e.target.value)} />
        <InputField label="Источник" value={source} onChange={(e) => setSource(e.target.value)} />
      </div>

      {loading && <TableSkeleton rows={6} cols={7} />}
      {error && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}
      {page && !loading && (
        <LotsTable
          items={page.content}
          totalElements={page.totalElements}
          onArchive={handleArchive}
          archivingId={archivingId}
        />
      )}
    </div>
  );
}
