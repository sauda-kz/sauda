import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { fetchCurrentOrganization, fetchMe, login as apiLogin } from "../api/auth";
import type { LoginRequest, MeResponse, OrganizationResponse } from "../types/api";

const TOKEN_KEY = "sauda_access_token";
const REFRESH_KEY = "sauda_refresh_token";

interface AuthContextValue {
  user: MeResponse | null;
  organization: OrganizationResponse | null;
  token: string | null;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [organization, setOrganization] = useState<OrganizationResponse | null>(null);
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY));
  const [isLoading, setIsLoading] = useState(Boolean(localStorage.getItem(TOKEN_KEY)));

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    setToken(null);
    setUser(null);
    setOrganization(null);
  }, []);

  const bootstrap = useCallback(
    async (accessToken: string) => {
      const me = await fetchMe(accessToken);
      const org = await fetchCurrentOrganization(accessToken);
      setUser(me);
      setOrganization(org);
      setToken(accessToken);
    },
    [],
  );

  useEffect(() => {
    if (!token) {
      setIsLoading(false);
      return;
    }

    let cancelled = false;

    bootstrap(token)
      .catch(() => {
        if (!cancelled) logout();
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [token, bootstrap, logout]);

  const login = useCallback(
    async (credentials: LoginRequest) => {
      const response = await apiLogin(credentials);
      localStorage.setItem(TOKEN_KEY, response.accessToken);
      localStorage.setItem(REFRESH_KEY, response.refreshToken);
      await bootstrap(response.accessToken);
    },
    [bootstrap],
  );

  const value = useMemo(
    () => ({ user, organization, token, isLoading, login, logout }),
    [user, organization, token, isLoading, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
