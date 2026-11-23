import { useState, type FormEvent } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Box } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { ApiError } from '../api/client';
import { Alert, Field } from '../components/ui';

const DEMO = [
  { email: 'admin@demo.local', label: 'Administrador' },
  { email: 'almacen@demo.local', label: 'Almacén' },
  { email: 'consulta@demo.local', label: 'Consulta' },
];

export function LoginPage() {
  const { user, login } = useAuth();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  if (user) {
    const from = (location.state as { from?: string } | null)?.from ?? '/';
    return <Navigate to={from} replace />;
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!email || !password) { setError('Escribe tu email y contraseña'); return; }
    setLoading(true);
    try {
      await login(email, password);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Error inesperado');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand"><span className="brand-logo lg"><Box size={26} /></span></div>
        <h1>Sistema de Inventario</h1>
        <p className="muted">Inicia sesión para administrar productos y existencias</p>
        <form onSubmit={submit} className="form" noValidate>
          {error && <Alert>{error}</Alert>}
          <Field label="Email">
            <input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus />
          </Field>
          <Field label="Contraseña">
            <input type="password" autoComplete="current-password" value={password}
              onChange={(e) => setPassword(e.target.value)} />
          </Field>
          <button className="btn btn-primary btn-block" disabled={loading}>{loading ? 'Entrando…' : 'Iniciar sesión'}</button>
        </form>
        <div className="demo-accounts">
          <p className="muted small">Cuentas de demostración (contraseña <code>Demo1234!</code>):</p>
          <div className="demo-buttons">
            {DEMO.map((d) => (
              <button key={d.email} type="button" className="btn btn-ghost btn-sm"
                onClick={() => { setEmail(d.email); setPassword('Demo1234!'); }}>{d.label}</button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
