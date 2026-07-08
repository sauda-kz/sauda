import { useEffect, useState } from "react";
import { fetchOffers } from "../../../../api/offers";
import { useAuth } from "../../../../auth/AuthProvider";

interface OfferStats {
  total: number;
  inStock: number;
  loading: boolean;
}

export function useOfferStats(): OfferStats {
  const { token } = useAuth();
  const [total, setTotal] = useState(0);
  const [inStock, setInStock] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    setLoading(true);

    Promise.all([
      fetchOffers(token, { page: 0, size: 1 }),
      fetchOffers(token, { page: 0, size: 1, stockStatus: "in_stock" }),
    ])
      .then(([all, available]) => {
        if (cancelled) return;
        setTotal(all.totalElements);
        setInStock(available.totalElements);
      })
      .catch(() => {
        if (!cancelled) {
          setTotal(0);
          setInStock(0);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  return { total, inStock, loading };
}
