import { apiRequest } from "./client";
import type { OfferPage, OffersListParams } from "../features/distributor/offers/types";

function buildQuery(params: OffersListParams): string {
  const search = new URLSearchParams();
  if (params.q) search.set("q", params.q);
  if (params.category) search.set("category", params.category);
  if (params.brand) search.set("brand", params.brand);
  if (params.stockStatus) search.set("stockStatus", params.stockStatus);
  if (params.page !== undefined) search.set("page", String(params.page));
  if (params.size !== undefined) search.set("size", String(params.size));
  const query = search.toString();
  return query ? `?${query}` : "";
}

export function fetchOffers(token: string, params: OffersListParams = {}) {
  return apiRequest<OfferPage>(`/offers${buildQuery(params)}`, { token });
}
