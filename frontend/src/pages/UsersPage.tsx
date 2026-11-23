import { useState, type FormEvent } from 'react';
import { Plus, Pencil } from 'lucide-react';
import { api, ApiError } from '../api/client';
import type { Role, User } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useAsync } from '../lib/useAsync';
import { ROLE_LABEL, fmtDateTime } from '../lib/format';
import { Alert, Card, Field, Loading, Modal, PageHeader } from '../components/ui';

export function UsersPage() {
  const { user: me } = useAuth();
  const { data, error, loading, reload } = useAsync(() => api.get<User[]>('/api/users'), []);
  const [editing, setEditing] = useState<User | 'new' | null>(null);

  return (
    <>
      <PageHeader title="Usuarios" subtitle="Cuentas y permisos por rol"
        actions={<button className="btn btn-primary" onClick={() => setEditing('new')}><Plus size={16} /> Nuevo usuario</button>} />
      {error && <Alert>{error}</Alert>}
      <Card>
        {loading && !data ? <Loading /> : data && (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Nombre</th><th>Email</th><th>Rol</th><th>Estado</th><th>Alta</th><th className="actions-col">Acciones</th></tr></thead>
              <tbody>
                {data.map((u) => (
                  <tr key={u.id}>
                    <td className="strong">{u.name}{u.id === me?.id && <span className="muted small"> (tú)</span>}</td>
                    <td>{u.email}</td>
                    <td><span className={`badge badge-${u.role === 'ADMIN' ? 'violet' : u.role === 'WAREHOUSE' ? 'blue' : 'gray'}`}>{ROLE_LABEL[u.role]}</span></td>
                    <td>{u.active ? <span className="badge badge-green">Activo</span> : <span className="badge badge-gray">Inactivo</span>}</td>
                    <td className="muted nowrap">{fmtDateTime(u.createdAt)}</td>
                    <td className="actions-col">
                      <button className="btn btn-ghost btn-icon" title="Editar" onClick={() => setEditing(u)}><Pencil size={16} /></button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      {editing && (
        <UserForm user={editing === 'new' ? null : editing} onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); reload(); }} />
      )}
    </>
  );
}

function UserForm({ user, onClose, onSaved }: { user: User | null; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState({
    name: user?.name ?? '', email: user?.email ?? '', password: '', role: user?.role ?? ('WAREHOUSE' as Role),
    active: user?.active ?? true,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    const v: Record<string, string> = {};
    if (!form.name.trim()) v.name = 'El nombre es obligatorio';
    if (!/^\S+@\S+\.\S+$/.test(form.email)) v.email = 'Email inválido';
    if (!user && form.password.length < 8) v.password = 'Mínimo 8 caracteres';
    if (user && form.password && form.password.length < 8) v.password = 'Mínimo 8 caracteres';
    setErrors(v);
    if (Object.keys(v).length) return;
    setSaving(true);
    setServerError(null);
    try {
      const body = { ...form, password: form.password || undefined };
      if (user) await api.put(`/api/users/${user.id}`, body);
      else await api.post('/api/users', body);
      onSaved();
    } catch (err) {
      if (err instanceof ApiError) { setErrors(err.fieldErrors); setServerError(err.message); }
      else setServerError('Error inesperado');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={user ? 'Editar usuario' : 'Nuevo usuario'} onClose={onClose}>
      <form className="form" onSubmit={submit} noValidate>
        {serverError && <Alert>{serverError}</Alert>}
        <Field label="Nombre" error={errors.name} required>
          <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </Field>
        <Field label="Email" error={errors.email} required>
          <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </Field>
        <Field label="Contraseña" error={errors.password} required={!user}
          hint={user ? 'Déjala vacía para conservar la actual' : 'Mínimo 8 caracteres'}>
          <input type="password" autoComplete="new-password" value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </Field>
        <div className="grid-2">
          <Field label="Rol" error={errors.role} required>
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
              {(Object.keys(ROLE_LABEL) as Role[]).map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}
            </select>
          </Field>
          <Field label="Estado">
            <select value={form.active ? '1' : '0'} onChange={(e) => setForm({ ...form, active: e.target.value === '1' })}>
              <option value="1">Activo</option>
              <option value="0">Inactivo</option>
            </select>
          </Field>
        </div>
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button>
          <button className="btn btn-primary" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</button>
        </div>
      </form>
    </Modal>
  );
}
