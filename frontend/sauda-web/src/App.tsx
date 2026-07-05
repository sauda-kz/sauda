import { BrowserRouter, Navigate, Route, Routes, useParams } from "react-router-dom";
import { AuthProvider } from "./auth/AuthProvider";
import { AdminRoute, DistributorRoute, ProtectedRoute } from "./auth/ProtectedRoute";
import { AdminLayout } from "./components/layout/AdminLayout";
import { DistributorLayout } from "./components/layout/DistributorLayout";
import { LotCreatePage } from "./features/admin/lots/pages/LotCreatePage";
import { AdminImportRunDetailPage } from "./features/admin/imports/pages/AdminImportRunDetailPage";
import { AdminImportRunsListPage } from "./features/admin/imports/pages/AdminImportRunsListPage";
import { LotDetailPage } from "./features/admin/lots/pages/LotDetailPage";
import { LotEditPage } from "./features/admin/lots/pages/LotEditPage";
import { LotsListPage } from "./features/admin/lots/pages/LotsListPage";
import { NotificationsPage } from "./features/distributor/notifications/pages/NotificationsPage";
import { ImportRunDetailPage } from "./features/distributor/imports/pages/ImportRunDetailPage";
import { ImportRunsListPage } from "./features/distributor/imports/pages/ImportRunsListPage";
import { SuitableLotDetailPage } from "./features/distributor/suitable-lots/pages/SuitableLotDetailPage";
import { SuitableLotsListPage } from "./features/distributor/suitable-lots/pages/SuitableLotsListPage";
import { LoginPage } from "./pages/LoginPage";

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
                <Route path="/admin/imports" element={<AdminImportRunsListPage />} />
                <Route
                  path="/admin/imports/:distributorId/:runId"
                  element={<AdminImportRunDetailPage />}
                />
              </Route>
            </Route>

            <Route element={<DistributorRoute />}>
              <Route element={<DistributorLayout />}>
                <Route path="/" element={<Navigate to="/suitable-lots" replace />} />
                <Route path="/lots" element={<Navigate to="/suitable-lots" replace />} />
                <Route path="/lots/:matchId" element={<LegacyLotRedirect />} />
                <Route path="/suitable-lots" element={<SuitableLotsListPage />} />
                <Route path="/suitable-lots/:matchId" element={<SuitableLotDetailPage />} />
                <Route path="/imports" element={<ImportRunsListPage />} />
                <Route path="/imports/:runId" element={<ImportRunDetailPage />} />
                <Route path="/notifications" element={<NotificationsPage />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

function LegacyLotRedirect() {
  const { matchId } = useParams<{ matchId: string }>();
  return <Navigate to={`/suitable-lots/${matchId}`} replace />;
}
