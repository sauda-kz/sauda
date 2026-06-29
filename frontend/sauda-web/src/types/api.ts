/** TypeScript — удобен для контрактов API (формы ответов Swagger). */

export type OrganizationType = "platform" | "distributor" | "buyer";

export type LotMatchStatus =
  | "suggested"
  | "matched"
  | "needs_review"
  | "not_matched"
  | "dismissed"
  | "interested"
  | "mismatch_reported";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
}

export interface MeResponse {
  userId: string;
  email: string;
  organizationId: string;
  organizationType: OrganizationType;
  roles: string[];
}

export interface OrganizationResponse {
  id: string;
  type: OrganizationType;
  name: string;
  bin: string | null;
  vatPayer: boolean;
}

export interface DistributorLotMatchCard {
  matchId: string;
  status: LotMatchStatus;
  title: string;
  customerName: string;
  budgetAmount: number;
  currency: string;
  deliveryDeadline: string | null;
  submissionDeadline: string | null;
  deliveryLocation: string | null;
  category: string | null;
  quantity: number | null;
  unit: string | null;
  requiredDocuments: string | null;
  sourceUrl: string | null;
  offerName: string | null;
  brand: string | null;
  modelMpn: string | null;
  availableQuantity: number;
  estimatedUnitPrice: number | null;
  estimatedTotalPrice: number | null;
  estimatedMargin: number | null;
  confidenceScore: number | null;
  matchReason: string | null;
  matchedRequirements: string[];
  missingRequirements: string[];
  riskFlags: string[];
  needsManualReview: boolean;
  distributorComment: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
    details?: unknown;
  };
  message?: string;
}
