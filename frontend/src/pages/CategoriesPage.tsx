import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Pencil, Trash2 } from 'lucide-react';
import { api, ApiError } from '../api/client';
import type { Category } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useAsync } from '../lib/useAsync';
import { fmtNumber } from '../lib/format';
import { Alert, Card, Empty, Field, Loading, Modal, PageHeader } from '../components/ui';

export function CategoriesPage() {
  const { can } = useAuth();
  const { data, error, loading, reload } = useAsync(() => api.get<Category[]>('/api/categories'), []);
  const [editing, setEditing] = useState<Category | 'new' | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  async function remove(c: Category) {
    if (!confirm(`¿Eliminar la categoría ${c.name}?`)) return;
    setActionError(null);
    try {
      await api.del(`/api/categories/${c.id}`);
      reload();
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : 'Error inesperado');
    }
  }

  return (
    <>
      <PageHeader title="Categorías" subtitle="Agrupación del catálogo"
        actions={can('ADMIN') && <button className="btn btn-primary" onClick={() => setEditing('new')}><Plus size={16} /> Nueva categoría</button>} />
      {(error || actionError) && <Alert>{error ?? actionError}</Alert>}
      <Card>
        {loading && !data ? <Loading /> : data && data.length === 0 ? <Empty>No hay categorías</Empty> : data && (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Nombre</th><th>Descripción</th><th className="num">Productos</th>{can('ADMIN') && <th className="actions-col">Acciones</th>}</tr></thead>
              <tbody>
                {data.map((c) => (
                  <tr key={c.id}>
                    <td className="strong"><Link to={`/productos?categoryId=${c.id}&status=all`} className="link-plain">{c.name}</Link></td>
                    <td className="muted">{c.description ?? '—'}</td>
                    <td className="num">{fmtNumber(c.productCount)}</td>
                    {can('ADMIN') && (
                      <td className="actions-col">
                        <button className="btn btn-ghost btn-icon" title="Editar" onClick={() => setEditing(c)}><Pencil size={16} /></button>
                        <button className="btn btn-ghost btn-icon" title={c.productCount > 0 ? 'Tiene productos asociados' : 'Eliminar'}
                          disabled={c.productCount > 0} onClick={() => remove(c)}><Trash2 size={16} /></button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      {editing && (
        <CategoryForm category={editing === 'new' ? null : editing} onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); reload(); }} />
      )}
    </>
  );
}

function CategoryForm({ category, onClose, onSaved }: { category: Category | null; onClose: () => void; onSaved: () => void }) {
  const [name, setName] = useState(category?.name ?? '');
  const [description, setDescription] = useState(category?.description ?? '');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) { setErrors({ name: 'El nombre es obligatorio' }); return; }
    setSaving(true);
    setServerError(null);
    try {
      const body = { name, description };
      if (category) await api.put(`/api/categories/${category.id}`, body);
      else await api.post('/api/categories', body);
      onSaved();
    } catch (err) {
      if (err instanceof ApiError) { setErrors(err.fieldErrors); setServerError(err.message); }
      else setServerError('Error inesperado');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={category ? 'Editar categoría' : 'Nueva categoría'} onClose={onClose}>
      <form className="form" onSubmit={submit} noValidate>
        {serverError && <Alert>{serverError}</Alert>}
        <Field label="Nombre" error={errors.name} required>
          <input value={name} maxLength={80} onChange={(e) => setName(e.target.value)} autoFocus />
        </Field>
        <Field label="Descripción" error={errors.description}>
          <input value={description} maxLength={255} onChange={(e) => setDescription(e.target.value)} />
        </Field>
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button>
          <button className="btn btn-primary" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</button>
        </div>
      </form>
    </Modal>
  );
}
