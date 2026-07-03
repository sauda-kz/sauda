import { apiRequest } from "./client";
import type {
  LoginRequest,
  LoginResponse,
  MeResponse,
  OrganizationResponse,
} from "../types/api";

export function login(request: LoginRequest) {
  return apiRequest<LoginResponse>("/auth/login", { method: "POST", body: request });
}

export function refresh(refreshToken: string) {
  return apiRequest<LoginResponse>("/auth/refresh", {
    method: "POST",
    body: { refreshToken },
  });
}

export function fetchMe(token: string) {
  return apiRequest<MeResponse>("/auth/me", { token });
}

export function fetchCurrentOrganization(token: string) {
  return apiRequest<OrganizationResponse>("/organizations/current", { token });
}
