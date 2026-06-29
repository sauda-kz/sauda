import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthProvider";
import { DistributorRoute, ProtectedRoute } from "./auth/ProtectedRoute";
import { DistributorLayout } from "./components/layout/DistributorLayout";
import { LoginPage } from "./pages/LoginPage";
import { LotDetailPage } from "./pages/distributor/LotDetailPage";
import { LotsPage } from "./pages/distributor/LotsPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<DistributorRoute />}>
              <Route element={<DistributorLayout />}>
                <Route path="/" element={<Navigate to="/lots" replace />} />
                <Route path="/lots" element={<LotsPage />} />
                <Route path="/lots/:matchId" element={<LotDetailPage />} />
              </Route>
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/lots" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
