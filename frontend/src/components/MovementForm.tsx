import { useEffect, useState, type FormEvent } from 'react';
import { api, ApiError } from '../api/client';
import type { Movement, MovementInput, MovementType, Page, Product } from '../api/types';
import { MOVEMENT_LABEL, fmtNumber } from '../lib/format';
import { Alert, Field, Modal } from './ui';

interface Props {
  product?: Product;              // si se abre desde el detalle, el producto viene fijo
  defaultType?: MovementType;
  onClose: () => void;
  onSaved: (m: Movement) => void;
}

/** Formulario de entrada, salida o ajuste. Valida en cliente y muestra los errores del servidor por campo. */
export function MovementForm({ product, defaultType = 'IN', onClose, onSaved }: Props) {
  const [form, setForm] = useState<MovementInput>({
    productId: product?.id ?? null, type: defaultType, quantity: null, direction: 'DECREASE', reason: '', reference: '',
  });
  const [products, setProducts] = useState<Product[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (product) return;
    api.get<Page<Product>>('/api/products', { active: true, size: 100, sort: 'name,asc' })
      .then((p) => setProducts(p.content))
      .catch(() => setServerError('No se pudo cargar la lista de productos'));
  }, [product]);

  const selected = product ?? products.find((p) => p.id === form.productId);
  const set = <K extends keyof MovementInput>(k: K, v: MovementInput[K]) => setForm((f) => ({ ...f, [k]: v }));

  const projected = (() => {
    if (!selected || !form.quantity) return null;
    const sign = form.type === 'IN' || (form.type === 'ADJUSTMENT' && form.direction === 'INCREASE') ? 1 : -1;
    return selected.stock + sign * form.quantity;
  })();

  function validate() {
    const e: Record<string, string> = {};
    if (!form.productId) e.productId = 'Selecciona un producto';
    if (!form.quantity || form.quantity <= 0 || !Number.isInteger(form.quantity)) {
      e.quantity = 'La cantidad debe ser un entero mayor que cero';
    }
    if (form.type === 'ADJUSTMENT' && (form.reason ?? '').trim().length < 5) {
      e.reason = 'Explica el motivo del ajuste (mínimo 5 caracteres)';
    }
    if (projected !== null && projected < 0) {
      e.quantity = `No hay stock suficiente: disponible ${fmtNumber(selected!.stock)}`;
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
      const body: MovementInput = { ...form, direction: form.type === 'ADJUSTMENT' ? form.direction : undefined };
      onSaved(await api.post<Movement>('/api/stock-movements', body));
    } catch (e) {
      if (e instanceof ApiError) {
        setErrors(e.fieldErrors);
        setServerError(e.message);
      } else setServerError('Error inesperado');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title="Registrar movimiento" onClose={onClose}>
      <form onSubmit={submit} className="form" noValidate>
        {serverError && <Alert>{serverError}</Alert>}

        <div className="segmented" role="radiogroup" aria-label="Tipo de movimiento">
          {(['IN', 'OUT', 'ADJUSTMENT'] as MovementType[]).map((t) => (
            <button type="button" key={t} role="radio" aria-checked={form.type === t}
              className={`seg seg-${t.toLowerCase()} ${form.type === t ? 'active' : ''}`} onClick={() => set('type', t)}>
              {MOVEMENT_LABEL[t]}
            </button>
          ))}
        </div>

        {product ? (
          <div className="readonly-product">
            <span className="mono">{product.sku}</span> {product.name}
          </div>
        ) : (
          <Field label="Producto" error={errors.productId} required>
            <select value={form.productId ?? ''} onChange={(e) => set('productId', e.target.value ? Number(e.target.value) : null)}>
              <option value="">Selecciona…</option>
              {products.map((p) => <option key={p.id} value={p.id}>{p.sku} · {p.name} (stock {p.stock})</option>)}
            </select>
          </Field>
        )}

        <div className="grid-2">
          <Field label="Cantidad" error={errors.quantity} required>
            <input type="number" min={1} step={1} inputMode="numeric" value={form.quantity ?? ''}
              onChange={(e) => set('quantity', e.target.value === '' ? null : Number(e.target.value))} />
          </Field>
          {form.type === 'ADJUSTMENT' ? (
            <Field label="Sentido del ajuste" error={errors.direction} required>
              <select value={form.direction} onChange={(e) => set('direction', e.target.value as 'INCREASE' | 'DECREASE')}>
                <option value="DECREASE">Disminuir (faltante, merma)</option>
                <option value="INCREASE">Aumentar (sobrante)</option>
              </select>
            </Field>
          ) : (
            <Field label="Referencia" error={errors.reference} hint="Factura, orden de compra…">
              <input value={form.reference} maxLength={100} onChange={(e) => set('reference', e.target.value)} />
            </Field>
          )}
        </div>

        {form.type === 'ADJUSTMENT' && (
          <Field label="Referencia" error={errors.reference} hint="Folio de conteo físico, acta…">
            <input value={form.reference} maxLength={100} onChange={(e) => set('reference', e.target.value)} />
          </Field>
        )}

        <Field label="Motivo" error={errors.reason} required={form.type === 'ADJUSTMENT'}
          hint={form.type === 'ADJUSTMENT' ? 'Obligatorio en ajustes' : 'Opcional'}>
          <input value={form.reason} maxLength={255} onChange={(e) => set('reason', e.target.value)} />
        </Field>

        {selected && (
          <div className="stock-preview">
            <div><span className="muted">Stock actual</span><strong>{fmtNumber(selected.stock)}</strong></div>
            <div><span className="muted">Stock resultante</span>
              <strong className={projected !== null && projected < 0 ? 'text-red' : ''}>
                {projected === null ? '—' : fmtNumber(projected)}
              </strong>
            </div>
            <div><span className="muted">Mínimo</span><strong>{fmtNumber(selected.minStock)}</strong></div>
          </div>
        )}

        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancelar</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Guardando…' : 'Registrar'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
