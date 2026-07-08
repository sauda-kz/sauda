/** Admin lots feature — synced with backend DTOs (SAUDA-070). */

export type LotStatus =
  | "draft"
  | "active"
  | "needs_review"
  | "expired"
  | "archived"
  | "cancelled"
  | "closed";

export type LotDataQualityStatus = "complete" | "incomplete";

export type CheckResult = "ok" | "fail" | "unknown";

export interface Lot {
  id: string;
  source: string | null;
  externalPurchaseId: string | null;
  externalLotId: string | null;
  title: string;
  description: string | null;
  customerName: string;
  category: string;
  procurementMethod: string | null;
  lotType: string | null;
  quantity: number | null;
  unit: string | null;
  budgetAmount: number | null;
  currency: string;
  deliveryLocation: string | null;
  deliveryDeadline: string | null;
  submissionDeadline: string | null;
  warrantyRequirements: string | null;
  technicalRequirements: string | null;
  requiredDocuments: string | null;
  qualificationRequirements: string | null;
  contractTermsSummary: string | null;
  publishedAt: string | null;
  status: LotStatus;
  sourceUrl: string | null;
  rawText: string | null;
  createdById: string | null;
  createdAt: string;
  updatedAt: string;
  dataQualityStatus: LotDataQualityStatus;
  missingKeyFields: string[];
  matchCount: number;
}

export interface LotFormValues {
  source: string;
  externalPurchaseId: string;
  externalLotId: string;
  title: string;
  description: string;
  customerName: string;
  category: string;
  procurementMethod: string;
  lotType: string;
  quantity: string;
  unit: string;
  budgetAmount: string;
  currency: string;
  deliveryLocation: string;
  deliveryDeadline: string;
  submissionDeadline: string;
  warrantyRequirements: string;
  technicalRequirements: string;
  requiredDocuments: string;
  qualificationRequirements: string;
  contractTermsSummary: string;
  publishedAt: string;
  status: LotStatus;
  sourceUrl: string;
  rawText: string;
}

export interface LotWritePayload extends Omit<LotFormValues, "quantity" | "budgetAmount"> {
  quantity: number | null;
  budgetAmount: number | null;
  confirmIncomplete?: boolean;
}

export interface IncompleteLotWarning {
  message: string;
  missingFields: string[];
}

export interface LotAttachment {
  id: string;
  lotId: string;
  originalFilename: string;
  fileSize: number;
  mimeType: string;
  uploadedById: string;
  createdAt: string;
}

export interface PotentialMatch {
  offerId: string;
  distributorId: string;
  distributorName: string;
  offerName: string;
  price: number | null;
  priceIncludesVat: boolean | null;
  stockQuantity: number | null;
  stockStatus: string | null;
  leadTime: string | null;
  matchReason: string;
  missingData: string[];
  recommendedStatus: string;
  confidenceScore: number;
  quantityCheck: CheckResult;
  stockCheck: CheckResult;
  priceCheck: CheckResult;
}

export interface AdminLotMatch {
  id: string;
  lotId: string;
  offerId: string;
  distributorId: string;
  status: string;
  confidenceScore: number | null;
  matchReason: string | null;
  matchedRequirements: string[];
  missingRequirements: string[];
  riskFlags: string[];
  requiredQuantity: number;
  availableQuantity: number;
  quantityCheck: CheckResult;
  stockCheck: CheckResult;
  priceCheck: CheckResult;
  estimatedUnitPrice: number | null;
  estimatedTotalPrice: number | null;
  budgetAmount: number | null;
  estimatedMargin: number | null;
  needsManualReview: boolean;
  adminComment: string;
  distributorComment: string | null;
  sentToDistributorAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SendToDistributorPayload {
  offerId: string;
  matchReason?: string;
  riskFlags?: string[];
  adminComment?: string;
  status?: "matched" | "needs_review";
}

export interface LotsListParams {
  q?: string;
  status?: LotStatus;
  category?: string;
  source?: string;
  page?: number;
  size?: number;
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
