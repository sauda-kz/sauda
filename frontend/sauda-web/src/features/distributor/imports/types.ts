import type { PageResponse } from "../../../types/api";

export type ImportStatus =
  | "pending"
  | "processing"
  | "parsed"
  | "parsed_with_errors"
  | "awaiting_approval"
  | "approved"
  | "rejected"
  | "applied"
  | "failed";

export type ParsedRowStatus = "valid" | "needs_review" | "error" | "edited";

export type StockStatus =
  | "in_stock"
  | "low_stock"
  | "out_of_stock"
  | "on_order"
  | "unknown";

export interface ImportRunResponse {
  id: string;
  rawUploadId: string;
  originalFilename: string;
  distributorId: string;
  adapterKey: string | null;
  status: ImportStatus;
  totalRows: number;
  parsedRowsCount: number;
  errorRowsCount: number;
  startedAt: string | null;
  finishedAt: string | null;
  approvedAt: string | null;
  rejectedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ParsedRowErrorItem {
  field: string | null;
  code: string | null;
  message: string;
}

export interface ParsedRowResponse {
  id: string;
  importRunId: string;
  sourceRowNumber: number | null;
  rawRowData: Record<string, unknown>;
  parsedData: Record<string, unknown>;
  status: ParsedRowStatus;
  errors: ParsedRowErrorItem[] | null;
  warnings: ParsedRowErrorItem[] | null;
  editedAt: string | null;
}

export interface UpdateParsedRowRequest {
  sku: string;
  name: string;
  brand?: string | null;
  mpn?: string | null;
  price?: number | null;
  priceIncludesVat?: boolean | null;
  stockQuantity?: number | null;
  stockStatus?: StockStatus | null;
  leadTimeDays?: number | null;
}

export interface RejectImportRequest {
  reason?: string | null;
}

export interface RawUploadResponse {
  id: string;
  distributorId: string;
  originalFilename: string;
  fileSize: number;
  mimeType: string;
  status: string;
  createdAt: string;
}

export type ImportRunPage = PageResponse<ImportRunResponse>;
export type ParsedRowPage = PageResponse<ParsedRowResponse>;

export const PROCESSING_IMPORT_STATUSES: ImportStatus[] = ["pending", "processing"];

export const DECISION_IMPORT_STATUSES: ImportStatus[] = [
  "awaiting_approval",
  "parsed",
  "parsed_with_errors",
];

export const STOCK_STATUS_LABELS: Record<StockStatus, string> = {
  in_stock: "В наличии",
  low_stock: "Мало",
  out_of_stock: "Нет в наличии",
  on_order: "Под заказ",
  unknown: "Неизвестно",
};

export const PARSED_ROW_STATUS_LABELS: Record<ParsedRowStatus, string> = {
  valid: "OK",
  needs_review: "Проверка",
  error: "Ошибка",
  edited: "Изменена",
};

export function parsedString(data: Record<string, unknown>, key: string): string {
  const value = data[key];
  if (value == null) return "";
  return String(value).trim();
}

export function parsedNumber(data: Record<string, unknown>, key: string): number | null {
  const value = data[key];
  if (value == null || value === "") return null;
  const num = Number(value);
  return Number.isFinite(num) ? num : null;
}

export function parsedBoolean(data: Record<string, unknown>, key: string): boolean | null {
  const value = data[key];
  if (value == null || value === "") return null;
  if (typeof value === "boolean") return value;
  return value === "true" || value === true;
}

export function rowToUpdateRequest(row: ParsedRowResponse): UpdateParsedRowRequest {
  const data = row.parsedData;
  return {
    sku: parsedString(data, "sku"),
    name: parsedString(data, "name"),
    brand: parsedString(data, "brand") || null,
    mpn: parsedString(data, "mpn") || null,
    price: parsedNumber(data, "price"),
    priceIncludesVat: parsedBoolean(data, "price_includes_vat"),
    stockQuantity: parsedNumber(data, "stock_quantity"),
    stockStatus: (parsedString(data, "stock_status") as StockStatus) || null,
    leadTimeDays: parsedNumber(data, "lead_time_days"),
  };
}
