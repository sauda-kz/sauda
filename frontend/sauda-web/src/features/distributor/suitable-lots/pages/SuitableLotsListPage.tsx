import { Inbox } from "lucide-react";
import { CompanyBanner } from "../../../../components/distributor/CompanyBanner";
import { StatsCards } from "../../../../components/distributor/StatsCards";
import { CardSkeletonList } from "../../../../components/ui/Skeleton";
import { useAuth } from "../../../../auth/AuthProvider";
import { useOfferStats } from "../../offers/hooks/useOfferStats";
import { SuitableLotCard } from "../components/SuitableLotCard";
import { useSuitableLots } from "../hooks/useSuitableLots";

export function SuitableLotsListPage() {
  const { organization } = useAuth();
  const { items, total, loading, error } = useSuitableLots();
  const offerStats = useOfferStats();

  if (!organization) return null;

  return (
    <div className="space-y-6">
      <CompanyBanner organization={organization} />
      <StatsCards
        totalMatches={total}
        uploadedProductsCount={offerStats.total}
        inStockCount={offerStats.inStock}
      />

      <div>
        <h1 className="text-2xl font-bold text-slate-900">Подходящие лоты</h1>
        <p className="mt-1 text-sm text-slate-500">
          Лоты, отправленные платформой для вашей компании
        </p>
      </div>

      {loading && <CardSkeletonList count={3} />}
      {error && (
        <p className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </p>
      )}
      {!loading && !error && items.length === 0 && (
        <div className="flex flex-col items-center rounded-xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-100 text-slate-400">
            <Inbox className="h-6 w-6" />
          </span>
          <p className="mt-4 text-sm font-medium text-slate-900">Подходящих лотов пока нет</p>
          <p className="mt-1 max-w-sm text-sm text-slate-500">
            Они появятся автоматически, как только платформа подберёт закупки под ваш прайс.
          </p>
        </div>
      )}
      {!loading && !error && items.length > 0 && (
        <div className="grid gap-4">
          {items.map((match) => (
            <SuitableLotCard key={match.matchId} match={match} />
          ))}
        </div>
      )}
    </div>
  );
}
