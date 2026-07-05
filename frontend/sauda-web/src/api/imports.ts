import { API_BASE, ApiError, apiRequest } from "./client";
import type {
  ImportRunPage,
  ImportRunResponse,
  ImportStatus,
  ParsedRowPage,
  ParsedRowResponse,
  ParsedRowStatus,
  RawUploadResponse,
  RejectImportRequest,
  UpdateParsedRowRequest,
} from "../features/distributor/imports/types";
import type { ApiErrorBody } from "../types/api";

function buildPageQuery(params?: {
  page?: number;
  size?: number;
  status?: ParsedRowStatus | ImportStatus;
  distributorId?: string;
}) {
  const search = new URLSearchParams();
  if (params?.page !== undefined) search.set("page", String(params.page));
  if (params?.size !== undefined) search.set("size", String(params.size));
  if (params?.status) search.set("status", params.status);
  if (params?.distributorId) search.set("distributorId", params.distributorId);
  const query = search.toString();
  return query ? `?${query}` : "";
}

export async function uploadRawFile(
  distributorId: string,
  file: File,
  token: string,
): Promise<RawUploadResponse> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE}/distributors/${distributorId}/raw-uploads`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: formData,
  });

  const text = await response.text();
  const parsed = text ? (JSON.parse(text) as unknown) : null;

  if (!response.ok) {
    const errorBody = parsed as ApiErrorBody | null;
    const message =
      errorBody?.error?.message ??
      errorBody?.message ??
      `Request failed (${response.status})`;
    throw new ApiError(response.status, message, errorBody);
  }

  return parsed as RawUploadResponse;
}

export function listAdminImportRuns(
  token: string,
  params?: { page?: number; size?: number; distributorId?: string; status?: ImportStatus },
) {
  return apiRequest<ImportRunPage>(`/import-runs${buildPageQuery(params)}`, { token });
}

export function listImportRuns(
  distributorId: string,
  token: string,
  params?: { page?: number; size?: number },
) {
  return apiRequest<ImportRunPage>(
    `/distributors/${distributorId}/import-runs${buildPageQuery(params)}`,
    { token },
  );
}

export function getImportRun(distributorId: string, runId: string, token: string) {
  return apiRequest<ImportRunResponse>(
    `/distributors/${distributorId}/import-runs/${runId}`,
    { token },
  );
}

export function listParsedRows(
  distributorId: string,
  runId: string,
  token: string,
  params?: { page?: number; size?: number; status?: ParsedRowStatus },
) {
  return apiRequest<ParsedRowPage>(
    `/distributors/${distributorId}/import-runs/${runId}/rows${buildPageQuery(params)}`,
    { token },
  );
}

export function updateParsedRow(
  distributorId: string,
  runId: string,
  rowId: string,
  token: string,
  payload: UpdateParsedRowRequest,
) {
  return apiRequest<ParsedRowResponse>(
    `/distributors/${distributorId}/import-runs/${runId}/rows/${rowId}`,
    { method: "PATCH", token, body: payload },
  );
}

export function approveImport(distributorId: string, runId: string, token: string) {
  return apiRequest<ImportRunResponse>(
    `/distributors/${distributorId}/import-runs/${runId}/approve`,
    { method: "POST", token },
  );
}

export function rejectImport(
  distributorId: string,
  runId: string,
  token: string,
  reason?: string,
) {
  const body: RejectImportRequest | undefined =
    reason && reason.trim() ? { reason: reason.trim() } : undefined;
  return apiRequest<ImportRunResponse>(
    `/distributors/${distributorId}/import-runs/${runId}/reject`,
    { method: "POST", token, body },
  );
}
