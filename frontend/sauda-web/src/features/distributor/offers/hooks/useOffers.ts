import { useCallback, useEffect, useState } from "react";
import { fetchOffers } from "../../../../api/offers";
import { ApiError } from "../../../../api/client";
import { useAuth } from "../../../../auth/AuthProvider";
import type { Offer, StockStatus } from "../types";

const PAGE_SIZE = 20;

export function useOffers() {
  const { token } = useAuth();
  const [items, setItems] = useState<Offer[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [q, setQ] = useState("");
  const [stockStatus, setStockStatus] = useState<StockStatus | "">("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      const result = await fetchOffers(token, {
        q: q.trim() || undefined,
        stockStatus: stockStatus || undefined,
        page,
        size: PAGE_SIZE,
      });
      setItems(result.content);
      setTotal(result.totalElements);
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка загрузки прайса");
    } finally {
      setLoading(false);
    }
  }, [token, q, stockStatus, page]);

  useEffect(() => {
    reload();
  }, [reload]);

  return {
    items,
    total,
    page,
    totalPages,
    q,
    stockStatus,
    loading,
    error,
    setPage,
    setQ: (value: string) => {
      setPage(0);
      setQ(value);
    },
    setStockStatus: (value: StockStatus | "") => {
      setPage(0);
      setStockStatus(value);
    },
    reload,
  };
}
