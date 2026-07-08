import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthProvider";
import { LogoMark } from "../components/ui/Logo";
import { Spinner } from "../components/ui/Spinner";

export function ProtectedRoute() {
  const { token, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="flex min-h-dvh flex-col items-center justify-center gap-4 bg-slate-50">
        <LogoMark className="h-12 w-12" />
        <Spinner className="h-5 w-5 text-brand-600" />
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

export function AdminRoute() {
  const { user, isLoading } = useAuth();

  if (isLoading) return null;

  if (user && user.organizationType !== "platform") {
    return <Navigate to="/login" replace state={{ error: "Доступ только для администратора" }} />;
  }

  return <Outlet />;
}
