import { apiRequest } from "./client";
import type {
  DistributorLotMatchCard,
  LotMatchStatus,
  PageResponse,
} from "../types/api";

export function fetchDistributorLotMatches(
  distributorId: string,
  token: string,
  params?: { status?: LotMatchStatus; page?: number; size?: number },
) {
  const search = new URLSearchParams();
  if (params?.status) search.set("status", params.status);
  if (params?.page !== undefined) search.set("page", String(params.page));
  if (params?.size !== undefined) search.set("size", String(params.size));
  const query = search.toString();

  return apiRequest<PageResponse<DistributorLotMatchCard>>(
    `/distributors/${distributorId}/lot-matches${query ? `?${query}` : ""}`,
    { token },
  );
}

export function fetchDistributorLotMatch(
  distributorId: string,
  matchId: string,
  token: string,
) {
  return apiRequest<DistributorLotMatchCard>(
    `/distributors/${distributorId}/lot-matches/${matchId}`,
    { token },
  );
}

export function updateLotMatchStatus(
  distributorId: string,
  matchId: string,
  token: string,
  status: LotMatchStatus,
  distributorComment?: string,
) {
  return apiRequest<DistributorLotMatchCard>(
    `/distributors/${distributorId}/lot-matches/${matchId}/status`,
    {
      method: "PATCH",
      token,
      body: { status, distributorComment: distributorComment ?? null },
    },
  );
}
