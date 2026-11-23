import { useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { ArrowLeft, Pencil, Plus, Power, Download } from 'lucide-react';
import { api, ApiError } from '../api/client';
import type { Movement, Page, Product } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useAsync } from '../lib/useAsync';
import { fmtDateTime, fmtDelta, fmtMoney, fmtNumber } from '../lib/format';
import { Alert, Card, Empty, Loading, MovementBadge, PageHeader, Pagination, StockBadge } from '../components/ui';
import { MovementForm } from '../components/MovementForm';

export function ProductDetailPage() {
  const { id } = useParams();
  const { can } = useAuth();
  const location = useLocation();
  const [flash, setFlash] = useState<string | null>((location.state as { flash?: string } | null)?.flash ?? null);
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const product = useAsync(() => api.get<Product>(`/api/products/${id}`), [id]);
  const history = useAsync(
    () => api.get<Page<Movement>>('/api/stock-movements', { productId: id, page, size: 10 }), [id, page]);

  async function toggleActive() {
    if (!product.data) return;
    const p = product.data;
    if (p.active && !confirm(`¿Desactivar ${p.name}? Se conservará su historial y podrás reactivarlo.`)) return;
    setActionError(null);
    try {
      const updated = p.active
        ? await api.del<Product>(`/api/products/${p.id}`)
        : await api.patch<Product>(`/api/products/${p.id}/activate`);
      product.setData(updated);
      setFlash(updated.active ? 'Producto reactivado' : 'Producto desactivado');
    } catch (e) {
      setActionError(e instanceof ApiError ? e.message : 'Error inesperado');
    }
  }

  if (product.error) return <><PageHeader title="Producto" /><Alert>{product.error}</Alert></>;
  if (!product.data) return <Loading />;
  const p = product.data;
  const ratio = Math.min(100, p.minStock > 0 ? (p.stock / (p.minStock * 3)) * 100 : 100);

  return (
    <>
      <PageHeader title={p.name} subtitle={`${p.sku} · ${p.categoryName}`}
        actions={<>
          <Link to="/productos" className="btn btn-ghost"><ArrowLeft size={16} /> Productos</Link>
          {can('ADMIN') && (
            <button className="btn btn-ghost" onClick={toggleActive}>
              <Power size={16} /> {p.active ? 'Desactivar' : 'Reactivar'}
            </button>
          )}
          {can('ADMIN', 'WAREHOUSE') && <>
            <Link to={`/productos/${p.id}/editar`} className="btn btn-ghost"><Pencil size={16} /> Editar</Link>
            {p.active && <button className="btn btn-primary" onClick={() => setShowForm(true)}><Plus size={16} /> Movimiento</button>}
          </>}
        </>} />

      {flash && <Alert tone="success">{flash}</Alert>}
      {actionError && <Alert>{actionError}</Alert>}

      <div className="detail-grid">
        <Card title="Stock actual">
          <div className="big-stock">
            <span className="big-number">{fmtNumber(p.stock)}</span>
            <span className="muted">unidades</span>
            <StockBadge stock={p.stock} min={p.minStock} active={p.active} />
          </div>
          <div className={`meter ${p.lowStock ? 'meter-low' : ''}`}><span style={{ width: `${ratio}%` }} /></div>
          <dl className="kv">
            <dt>Stock mínimo</dt><dd>{fmtNumber(p.minStock)}</dd>
            <dt>Valor a costo</dt><dd>{fmtMoney(p.stockValue)}</dd>
          </dl>
        </Card>
        <Card title="Información">
          <dl className="kv">
            <dt>SKU</dt><dd className="mono">{p.sku}</dd>
            <dt>Categoría</dt><dd>{p.categoryName}</dd>
            <dt>Costo</dt><dd>{fmtMoney(p.cost)}</dd>
            <dt>Precio</dt><dd>{fmtMoney(p.price)}</dd>
            <dt>Actualizado</dt><dd>{fmtDateTime(p.updatedAt)}</dd>
          </dl>
          {p.description && <p className="description">{p.description}</p>}
        </Card>
      </div>

      <Card title="Historial de movimientos"
        actions={<button className="btn btn-ghost btn-sm" onClick={() =>
          api.download('/api/reports/movements/export', { productId: p.id }, `movimientos-${p.sku}.csv`)}>
          <Download size={14} /> CSV</button>}>
        {history.error && <Alert>{history.error}</Alert>}
        {history.loading && !history.data ? <Loading /> : history.data && history.data.content.length === 0 ? (
          <Empty>Este producto aún no tiene movimientos</Empty>
        ) : history.data && (
          <>
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Fecha</th><th>Tipo</th><th className="num">Cantidad</th><th className="num">Antes</th>
                  <th className="num">Después</th><th>Motivo</th><th>Referencia</th><th>Usuario</th></tr></thead>
                <tbody>
                  {history.data.content.map((m) => (
                    <tr key={m.id}>
                      <td className="nowrap muted">{fmtDateTime(m.createdAt)}</td>
                      <td><MovementBadge type={m.type} delta={m.delta} /></td>
                      <td className={`num ${m.delta > 0 ? 'text-green' : 'text-red'}`}>{fmtDelta(m.delta)}</td>
                      <td className="num muted">{fmtNumber(m.stockBefore)}</td>
                      <td className="num strong">{fmtNumber(m.stockAfter)}</td>
                      <td>{m.reason ?? '—'}</td>
                      <td className="mono small">{m.reference ?? '—'}</td>
                      <td>{m.userName}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={history.data.page} totalPages={history.data.totalPages}
              totalElements={history.data.totalElements} size={history.data.size} onPage={setPage} />
          </>
        )}
      </Card>

      {showForm && (
        <MovementForm product={p} onClose={() => setShowForm(false)}
          onSaved={(m) => {
            setShowForm(false);
            setFlash(`Movimiento registrado. Stock: ${fmtNumber(m.stockBefore)} → ${fmtNumber(m.stockAfter)}`);
            product.reload();
            if (page === 0) history.reload(); else setPage(0);
          }} />
      )}
    </>
  );
}
