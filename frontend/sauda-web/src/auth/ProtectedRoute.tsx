import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthProvider";

export function ProtectedRoute() {
  const { token, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 text-slate-500">
        Загрузка…
      </div>
    );
  }

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

export function DistributorRoute() {
  const { user, isLoading } = useAuth();

  if (isLoading) return null;

  if (user && user.organizationType !== "distributor") {
    return <Navigate to="/login" replace state={{ error: "Доступ только для дистрибьютора" }} />;
  }

  return <Outlet />;
}
