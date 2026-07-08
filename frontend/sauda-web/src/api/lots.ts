import type {
  IncompleteLotWarning,
  Lot,
  LotFormValues,
  LotsListParams,
  PageResponse,
  PotentialMatch,
  SendToDistributorPayload,
} from "../features/admin/lots/types";
import type { AdminLotMatch } from "../features/admin/lots/types";
import { ApiError, apiRequest } from "./client";

export class IncompleteLotError extends Error {
  warning: IncompleteLotWarning;

  constructor(warning: IncompleteLotWarning) {
    super(warning.message);
    this.name = "IncompleteLotError";
    this.warning = warning;
  }
}

function toIsoOrNull(value: string): string | null {
  if (!value.trim()) return null;
  return new Date(value).toISOString();
}

function toNumberOrNull(value: string): number | null {
  if (!value.trim()) return null;
  const n = Number(value);
  return Number.isNaN(n) ? null : n;
}

export function formValuesToPayload(
  values: LotFormValues,
  confirmIncomplete?: boolean,
) {
  return {
    source: values.source || null,
    externalPurchaseId: values.externalPurchaseId || null,
    externalLotId: values.externalLotId || null,
    title: values.title,
    description: values.description || null,
    customerName: values.customerName,
    category: values.category,
    procurementMethod: values.procurementMethod || null,
    lotType: values.lotType || null,
    quantity: toNumberOrNull(values.quantity),
    unit: values.unit || null,
    budgetAmount: toNumberOrNull(values.budgetAmount),
    currency: values.currency || "KZT",
    deliveryLocation: values.deliveryLocation || null,
    deliveryDeadline: toIsoOrNull(values.deliveryDeadline),
    submissionDeadline: toIsoOrNull(values.submissionDeadline),
    warrantyRequirements: values.warrantyRequirements || null,
    technicalRequirements: values.technicalRequirements || null,
    requiredDocuments: values.requiredDocuments || null,
    qualificationRequirements: values.qualificationRequirements || null,
    contractTermsSummary: values.contractTermsSummary || null,
    publishedAt: toIsoOrNull(values.publishedAt),
    status: values.status,
    sourceUrl: values.sourceUrl || null,
    rawText: values.rawText || null,
    confirmIncomplete: confirmIncomplete ?? undefined,
  };
}

export function lotToFormValues(lot: Lot): LotFormValues {
  const toLocal = (iso: string | null) =>
    iso ? iso.slice(0, 16) : "";

  return {
    source: lot.source ?? "",
    externalPurchaseId: lot.externalPurchaseId ?? "",
    externalLotId: lot.externalLotId ?? "",
    title: lot.title,
    description: lot.description ?? "",
    customerName: lot.customerName,
    category: lot.category,
    procurementMethod: lot.procurementMethod ?? "",
    lotType: lot.lotType ?? "",
    quantity: lot.quantity != null ? String(lot.quantity) : "",
    unit: lot.unit ?? "",
    budgetAmount: lot.budgetAmount != null ? String(lot.budgetAmount) : "",
    currency: lot.currency ?? "KZT",
    deliveryLocation: lot.deliveryLocation ?? "",
    deliveryDeadline: toLocal(lot.deliveryDeadline),
    submissionDeadline: toLocal(lot.submissionDeadline),
    warrantyRequirements: lot.warrantyRequirements ?? "",
    technicalRequirements: lot.technicalRequirements ?? "",
    requiredDocuments: lot.requiredDocuments ?? "",
    qualificationRequirements: lot.qualificationRequirements ?? "",
    contractTermsSummary: lot.contractTermsSummary ?? "",
    publishedAt: toLocal(lot.publishedAt),
    status: lot.status,
    sourceUrl: lot.sourceUrl ?? "",
    rawText: lot.rawText ?? "",
  };
}

export const emptyLotFormValues = (): LotFormValues => ({
  source: "manual",
  externalPurchaseId: "",
  externalLotId: "",
  title: "",
  description: "",
  customerName: "",
  category: "",
  procurementMethod: "",
  lotType: "товар",
  quantity: "",
  unit: "шт",
  budgetAmount: "",
  currency: "KZT",
  deliveryLocation: "",
  deliveryDeadline: "",
  submissionDeadline: "",
  warrantyRequirements: "",
  technicalRequirements: "",
  requiredDocuments: "",
  qualificationRequirements: "",
  contractTermsSummary: "",
  publishedAt: "",
  status: "active",
  sourceUrl: "",
  rawText: "",
});

function buildQuery(params: LotsListParams): string {
  const search = new URLSearchParams();
  if (params.q) search.set("q", params.q);
  if (params.status) search.set("status", params.status);
  if (params.category) search.set("category", params.category);
  if (params.source) search.set("source", params.source);
  if (params.page !== undefined) search.set("page", String(params.page));
  if (params.size !== undefined) search.set("size", String(params.size));
  const query = search.toString();
  return query ? `?${query}` : "";
}

async function writeLot<T>(
  path: string,
  method: "POST" | "PUT",
  payload: ReturnType<typeof formValuesToPayload>,
  token: string,
): Promise<T> {
  try {
    return await apiRequest<T>(path, { method, token, body: payload });
  } catch (err) {
    if (err instanceof ApiError && err.status === 422 && err.body) {
      const warning = err.body as unknown as IncompleteLotWarning;
      if (warning.message && Array.isArray(warning.missingFields)) {
        throw new IncompleteLotError(warning);
      }
    }
    throw err;
  }
}

export function createLot(
  payload: ReturnType<typeof formValuesToPayload>,
  token: string,
) {
  return writeLot<Lot>("/lots", "POST", payload, token);
}

export function updateLot(
  id: string,
  payload: ReturnType<typeof formValuesToPayload>,
  token: string,
) {
  return writeLot<Lot>(`/lots/${id}`, "PUT", payload, token);
}
export function fetchLots(token: string, params: LotsListParams = {}) {
  return apiRequest<PageResponse<Lot>>(`/lots${buildQuery(params)}`, { token });
}

export function fetchLot(id: string, token: string) {
  return apiRequest<Lot>(`/lots/${id}`, { token });
}

export function archiveLot(id: string, token: string) {
  return apiRequest<Lot>(`/lots/${id}/archive`, { method: "PATCH", token });
}

export function fetchPotentialMatches(lotId: string, token: string) {
  return apiRequest<PotentialMatch[]>(`/lots/${lotId}/potential-matches`, { token });
}

export function sendLotToDistributor(
  lotId: string,
  payload: SendToDistributorPayload,
  token: string,
) {
  return apiRequest<AdminLotMatch>(`/lots/${lotId}/send-to-distributor`, {
    method: "POST",
    token,
    body: payload,
  });
}

export function fetchLotMatches(lotId: string, token: string, page = 0, size = 20) {
  const query = new URLSearchParams({
    lotId,
    page: String(page),
    size: String(size),
  });
  return apiRequest<PageResponse<AdminLotMatch>>(`/lot-matches?${query}`, { token });
}
