import { useState, type FormEvent } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { Button } from "../components/ui/Button";
import { ApiError } from "../api/client";

export function LoginPage() {
  const { login, token, user } = useAuth();
  const location = useLocation();
  const stateError = (location.state as { error?: string } | null)?.error;

  const [email, setEmail] = useState("dist@technodist.kz");
  const [password, setPassword] = useState("Sauda123!");
  const [error, setError] = useState(stateError ?? "");
  const [loading, setLoading] = useState(false);

  if (token && user?.organizationType === "distributor") {
    return <Navigate to="/lots" replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await login({ email, password });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось войти");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <div className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-md">
          <div className="mb-8 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-brand-600 text-lg font-bold text-white">
              S
            </div>
            <h1 className="mt-4 text-2xl font-bold text-slate-900">Вход в Sauda</h1>
            <p className="mt-2 text-sm text-slate-500">Кабинет дистрибьютора</p>
          </div>

          <form
            onSubmit={handleSubmit}
            className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
          >
            {error && (
              <p className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
            )}

            <label className="block text-sm font-medium text-slate-700">
              Email
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
              />
            </label>

            <label className="mt-4 block text-sm font-medium text-slate-700">
              Пароль
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
              />
            </label>

            <Button type="submit" loading={loading} className="mt-6 w-full">
              Войти
            </Button>

            <p className="mt-4 text-center text-xs text-slate-400">
              Demo: dist@technodist.kz / Sauda123!
            </p>
          </form>
        </div>
      </div>
    </div>
  );
}
