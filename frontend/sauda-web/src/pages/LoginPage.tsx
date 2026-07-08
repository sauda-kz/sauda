import { useState, type FormEvent } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { AlertCircle, Eye, EyeOff } from "lucide-react";
import { useAuth } from "../auth/AuthProvider";
import { Button } from "../components/ui/Button";
import { Logo } from "../components/ui/Logo";
import { ApiError } from "../api/client";

export function LoginPage() {
  const { login, token, user } = useAuth();
  const location = useLocation();
  const stateError = (location.state as { error?: string } | null)?.error;

  const [email, setEmail] = useState("distributor-a@shop.kz");
  const [password, setPassword] = useState("Sauda123!");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(stateError ?? "");
  const [loading, setLoading] = useState(false);

  if (token && user?.organizationType === "distributor") {
    return <Navigate to="/suitable-lots" replace />;
  }
  if (token && user?.organizationType === "platform") {
    return <Navigate to="/admin/lots" replace />;
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

  const inputClass =
    "w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 shadow-xs outline-none transition-colors placeholder:text-slate-400 hover:border-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/25";

  return (
    <div className="flex min-h-dvh items-center justify-center bg-slate-50 px-4 py-12 sm:px-6">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <Logo className="justify-center" wordClassName="text-2xl" markClassName="h-11 w-11" />
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-lg">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Вход в систему</h1>
          <p className="mt-1.5 text-sm text-slate-500">
            Войдите в кабинет платформы или дистрибьютора
          </p>

          <form onSubmit={handleSubmit} className="mt-8 space-y-5">
            {error && (
              <div
                role="alert"
                className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700"
              >
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-slate-700">
                Email
              </label>
              <input
                id="email"
                type="email"
                autoComplete="username"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={`mt-1.5 ${inputClass}`}
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-slate-700">
                Пароль
              </label>
              <div className="relative mt-1.5">
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className={`${inputClass} pr-11`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? "Скрыть пароль" : "Показать пароль"}
                  className="absolute inset-y-0 right-0 flex w-11 cursor-pointer items-center justify-center text-slate-400 transition-colors hover:text-slate-600"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <Button type="submit" loading={loading} className="w-full">
              Войти
            </Button>
          </form>
        </div>

        <p className="mt-6 text-center text-xs text-slate-400">
          Продолжая, вы принимаете условия использования платформы Sauda.
        </p>
      </div>
    </div>
  );
}
