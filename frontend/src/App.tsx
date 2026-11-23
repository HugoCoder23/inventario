import type { ReactNode } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import type { Role } from './api/types';
import { Layout } from './components/Layout';
import { Loading } from './components/ui';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { ProductsPage } from './pages/ProductsPage';
import { ProductFormPage } from './pages/ProductFormPage';
import { ProductDetailPage } from './pages/ProductDetailPage';
import { MovementsPage } from './pages/MovementsPage';
import { ReportsPage } from './pages/ReportsPage';
import { CategoriesPage } from './pages/CategoriesPage';
import { UsersPage } from './pages/UsersPage';

function RequireAuth({ children, roles }: { children: ReactNode; roles?: Role[] }) {
  const { user, ready, can } = useAuth();
  const location = useLocation();
  if (!ready) return <Loading />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  if (roles && !can(...roles)) return <Navigate to="/" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<RequireAuth><Layout /></RequireAuth>}>
            <Route index element={<DashboardPage />} />
            <Route path="productos" element={<ProductsPage />} />
            <Route path="productos/nuevo" element={<RequireAuth roles={['ADMIN', 'WAREHOUSE']}><ProductFormPage /></RequireAuth>} />
            <Route path="productos/:id" element={<ProductDetailPage />} />
            <Route path="productos/:id/editar" element={<RequireAuth roles={['ADMIN', 'WAREHOUSE']}><ProductFormPage /></RequireAuth>} />
            <Route path="movimientos" element={<MovementsPage />} />
            <Route path="reportes" element={<ReportsPage />} />
            <Route path="categorias" element={<CategoriesPage />} />
            <Route path="usuarios" element={<RequireAuth roles={['ADMIN']}><UsersPage /></RequireAuth>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
