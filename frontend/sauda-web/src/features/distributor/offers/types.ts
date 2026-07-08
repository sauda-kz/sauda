import type { PageResponse } from "../../../types/api";

export type StockStatus =
  | "in_stock"
  | "low_stock"
  | "out_of_stock"
  | "on_order"
  | "unknown";

export interface Offer {
  id: string;
  distributorId: string;
  distributorName: string | null;
  rawName: string;
  brand: string | null;
  modelMpn: string | null;
  category: string | null;
  price: number | null;
  currency: string;
  priceIncludesVat: boolean | null;
  stockQuantity: number | null;
  stockStatus: StockStatus | null;
  leadTime: string | null;
  lastUpdatedAt: string | null;
  createdAt: string | null;
}

export type OfferPage = PageResponse<Offer>;

export interface OffersListParams {
  q?: string;
  category?: string;
  brand?: string;
  stockStatus?: StockStatus;
  page?: number;
  size?: number;
}
