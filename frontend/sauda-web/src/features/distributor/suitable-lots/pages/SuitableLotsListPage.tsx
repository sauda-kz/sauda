import { CompanyBanner } from "../../../../components/distributor/CompanyBanner";
import { StatsCards } from "../../../../components/distributor/StatsCards";
import { useAuth } from "../../../../auth/AuthProvider";
import { SuitableLotCard } from "../components/SuitableLotCard";
import { useSuitableLots } from "../hooks/useSuitableLots";

export function SuitableLotsListPage() {
  const { organization } = useAuth();
  const { items, total, loading, error } = useSuitableLots();

  if (!organization) return null;

  return (
    <div className="space-y-6">
      <CompanyBanner organization={organization} />
      <StatsCards totalMatches={total} uploadedProductsCount={2} inStockCount={2} />

      <div>
        <h1 className="text-2xl font-bold text-slate-900">Подходящие лоты</h1>
        <p className="mt-1 text-sm text-slate-500">
          Лоты, отправленные платформой для вашей компании
        </p>
      </div>

      {loading && <p className="text-center text-sm text-slate-500">Загрузка…</p>}
      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}
      {!loading && !error && items.length === 0 && (
        <p className="rounded-xl border border-dashed border-slate-200 bg-white py-16 text-center text-sm text-slate-500">
          Подходящих лотов пока нет. Они появятся после отправки администратором.
        </p>
      )}
      <div className="grid gap-4">
        {items.map((match) => (
          <SuitableLotCard key={match.matchId} match={match} />
        ))}
      </div>
    </div>
  );
}
