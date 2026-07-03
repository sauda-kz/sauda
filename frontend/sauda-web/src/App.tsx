import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthProvider";
import { AdminRoute, DistributorRoute, ProtectedRoute } from "./auth/ProtectedRoute";
import { AdminLayout } from "./components/layout/AdminLayout";
import { DistributorLayout } from "./components/layout/DistributorLayout";
import { LotCreatePage } from "./features/admin/lots/pages/LotCreatePage";
import { LotDetailPage } from "./features/admin/lots/pages/LotDetailPage";
import { LotEditPage } from "./features/admin/lots/pages/LotEditPage";
import { LotsListPage } from "./features/admin/lots/pages/LotsListPage";
import { LoginPage } from "./pages/LoginPage";
import { LotDetailPage as DistributorLotDetailPage } from "./pages/distributor/LotDetailPage";
import { LotsPage } from "./pages/distributor/LotsPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AdminRoute />}>
              <Route element={<AdminLayout />}>
                <Route path="/admin" element={<Navigate to="/admin/lots" replace />} />
                <Route path="/admin/lots" element={<LotsListPage />} />
                <Route path="/admin/lots/new" element={<LotCreatePage />} />
                <Route path="/admin/lots/:id" element={<LotDetailPage />} />
                <Route path="/admin/lots/:id/edit" element={<LotEditPage />} />
              </Route>
            </Route>

            <Route element={<DistributorRoute />}>
              <Route element={<DistributorLayout />}>
                <Route path="/" element={<Navigate to="/lots" replace />} />
                <Route path="/lots" element={<LotsPage />} />
                <Route path="/lots/:matchId" element={<DistributorLotDetailPage />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
