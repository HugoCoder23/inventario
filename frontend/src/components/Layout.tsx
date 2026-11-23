import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  Box, LayoutDashboard, Package, ArrowLeftRight, BarChart3, Tags, Users, LogOut, Menu, X,
} from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { ROLE_LABEL } from '../lib/format';

export function Layout() {
  const { user, logout, can } = useAuth();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const [lastPath, setLastPath] = useState(location.pathname);
  if (lastPath !== location.pathname) { setLastPath(location.pathname); setOpen(false); }

  const links = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
    { to: '/productos', label: 'Productos', icon: Package },
    { to: '/movimientos', label: 'Movimientos', icon: ArrowLeftRight },
    { to: '/reportes', label: 'Reportes', icon: BarChart3 },
    { to: '/categorias', label: 'Categorías', icon: Tags },
    ...(can('ADMIN') ? [{ to: '/usuarios', label: 'Usuarios', icon: Users }] : []),
  ];

  return (
    <div className="shell">
      <header className="topbar">
        <button className="btn btn-ghost btn-icon" onClick={() => setOpen(!open)} aria-label="Menú">
          {open ? <X size={20} /> : <Menu size={20} />}
        </button>
        <span className="brand-inline"><Box size={18} /> Inventario</span>
      </header>
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand"><span className="brand-logo"><Box size={20} /></span> Inventario</div>
        <nav>
          {links.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              <Icon size={18} /> {label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          {user && (
            <div className="user-chip">
              <div className="avatar">{user.name.charAt(0)}</div>
              <div className="user-meta">
                <div className="user-name">{user.name}</div>
                <div className="user-role">{ROLE_LABEL[user.role]}</div>
              </div>
            </div>
          )}
          <button className="nav-link logout" onClick={logout}><LogOut size={18} /> Cerrar sesión</button>
        </div>
      </aside>
      {open && <div className="sidebar-scrim" onClick={() => setOpen(false)} />}
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
