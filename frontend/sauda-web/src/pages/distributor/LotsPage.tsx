import { useEffect, useState } from "react";
import { fetchDistributorLotMatches } from "../../api/lotMatches";
import { useAuth } from "../../auth/AuthProvider";
import { CompanyBanner } from "../../components/distributor/CompanyBanner";
import { LotsTable } from "../../components/distributor/LotsTable";
import { StatsCards } from "../../components/distributor/StatsCards";
import type { DistributorLotMatchCard } from "../../types/api";
import { ApiError } from "../../api/client";

export function LotsPage() {
  const { token, user, organization } = useAuth();
  const [items, setItems] = useState<DistributorLotMatchCard[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token || !user?.organizationId) return;

    let cancelled = false;

    fetchDistributorLotMatches(user.organizationId, token, { page: 0, size: 20 })
      .then((page) => {
        if (!cancelled) {
          setItems(page.content);
          setTotal(page.totalElements);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : "Ошибка загрузки");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [token, user?.organizationId]);

  if (!organization) return null;

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-4 py-8 sm:px-6 lg:px-8">
      <CompanyBanner organization={organization} />
      <StatsCards totalMatches={total} />

      {loading && (
        <p className="text-center text-sm text-slate-500">Загрузка лотов…</p>
      )}
      {error && (
        <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>
      )}
      {!loading && !error && (
        <LotsTable items={items} totalElements={total} pageSize={20} />
      )}
    </div>
  );
}
