import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { api, ApiError } from '../api/client';
import type { Category, Product, ProductInput } from '../api/types';
import { useAsync } from '../lib/useAsync';
import { Alert, Card, Field, Loading, PageHeader } from '../components/ui';

const EMPTY: ProductInput = {
  sku: '', name: '', description: '', categoryId: null, cost: null, price: null, minStock: 0, initialStock: 0,
};

export function ProductFormPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const [form, setForm] = useState<ProductInput>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [loaded, setLoaded] = useState(!editing);

  const categories = useAsync(() => api.get<Category[]>('/api/categories'), []);

  useEffect(() => {
    if (!id) return;
    api.get<Product>(`/api/products/${id}`)
      .then((p) => {
        setForm({ sku: p.sku, name: p.name, description: p.description ?? '', categoryId: p.categoryId,
          cost: p.cost, price: p.price, minStock: p.minStock });
        setLoaded(true);
      })
      .catch((e) => setServerError(e instanceof ApiError ? e.message : 'Error inesperado'));
  }, [id]);

  const set = <K extends keyof ProductInput>(k: K, v: ProductInput[K]) => {
    setForm((f) => ({ ...f, [k]: v }));
    setErrors((e) => ({ ...e, [k]: '' }));
  };
  const num = (v: string) => (v === '' ? null : Number(v));

  function validate() {
    const e: Record<string, string> = {};
    if (!form.sku.trim()) e.sku = 'El SKU es obligatorio';
    else if (!/^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(form.sku.trim())) e.sku = 'Solo letras, números, punto, guion y guion bajo';
    if (!form.name.trim()) e.name = 'El nombre es obligatorio';
    if (!form.categoryId) e.categoryId = 'Selecciona una categoría';
    if (form.cost === null || form.cost < 0) e.cost = 'Indica un costo mayor o igual a 0';
    if (form.price === null || form.price < 0) e.price = 'Indica un precio mayor o igual a 0';
    if (form.minStock === null || form.minStock < 0 || !Number.isInteger(form.minStock)) e.minStock = 'Entero mayor o igual a 0';
    if (!editing && form.initialStock != null && (form.initialStock < 0 || !Number.isInteger(form.initialStock))) {
      e.initialStock = 'Entero mayor o igual a 0';
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function submit(ev: FormEvent) {
    ev.preventDefault();
    setServerError(null);
    if (!validate()) return;
    setSaving(true);
    try {
      const saved = editing
        ? await api.put<Product>(`/api/products/${id}`, form)
        : await api.post<Product>('/api/products', form);
      navigate(`/productos/${saved.id}`, { state: { flash: editing ? 'Producto actualizado' : 'Producto creado' } });
    } catch (e) {
      if (e instanceof ApiError) {
        setErrors(e.fieldErrors);
        if (e.code === 'DUPLICATE_SKU') setErrors((x) => ({ ...x, sku: e.message }));
        setServerError(e.message);
      } else setServerError('Error inesperado');
    } finally {
      setSaving(false);
    }
  }

  const margin = form.cost && form.price ? ((form.price - form.cost) / form.price) * 100 : null;

  return (
    <>
      <PageHeader title={editing ? 'Editar producto' : 'Nuevo producto'}
        actions={<Link to={editing ? `/productos/${id}` : '/productos'} className="btn btn-ghost"><ArrowLeft size={16} /> Volver</Link>} />
      <Card className="form-card">
        {!loaded ? (serverError ? <Alert>{serverError}</Alert> : <Loading />) : (
          <form onSubmit={submit} className="form" noValidate>
            {serverError && <Alert>{serverError}</Alert>}
            <div className="grid-2">
              <Field label="SKU" error={errors.sku} required hint="Código único; se guarda en mayúsculas">
                <input value={form.sku} maxLength={40} onChange={(e) => set('sku', e.target.value)} className="mono" />
              </Field>
              <Field label="Categoría" error={errors.categoryId} required>
                <select value={form.categoryId ?? ''} onChange={(e) => set('categoryId', num(e.target.value))}>
                  <option value="">Selecciona…</option>
                  {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </Field>
            </div>
            <Field label="Nombre" error={errors.name} required>
              <input value={form.name} maxLength={150} onChange={(e) => set('name', e.target.value)} />
            </Field>
            <Field label="Descripción" error={errors.description}>
              <textarea rows={3} value={form.description} maxLength={1000} onChange={(e) => set('description', e.target.value)} />
            </Field>
            <div className="grid-3">
              <Field label="Costo" error={errors.cost} required>
                <input type="number" min={0} step="0.01" value={form.cost ?? ''} onChange={(e) => set('cost', num(e.target.value))} />
              </Field>
              <Field label="Precio de venta" error={errors.price} required
                hint={margin !== null && Number.isFinite(margin) ? `Margen ${margin.toFixed(1)} %` : undefined}>
                <input type="number" min={0} step="0.01" value={form.price ?? ''} onChange={(e) => set('price', num(e.target.value))} />
              </Field>
              <Field label="Stock mínimo" error={errors.minStock} required hint="Umbral de alerta">
                <input type="number" min={0} step={1} value={form.minStock ?? ''} onChange={(e) => set('minStock', num(e.target.value))} />
              </Field>
            </div>
            {!editing && (
              <Field label="Stock inicial" error={errors.initialStock}
                hint="Se registra como un movimiento de entrada «Stock inicial»">
                <input type="number" min={0} step={1} value={form.initialStock ?? ''} onChange={(e) => set('initialStock', num(e.target.value))} />
              </Field>
            )}
            {editing && <p className="muted small">El stock no se edita aquí: registra una entrada, salida o ajuste desde el detalle del producto.</p>}
            <div className="form-actions">
              <Link to={editing ? `/productos/${id}` : '/productos'} className="btn btn-ghost">Cancelar</Link>
              <button className="btn btn-primary" disabled={saving}>{saving ? 'Guardando…' : 'Guardar'}</button>
            </div>
          </form>
        )}
      </Card>
    </>
  );
}
