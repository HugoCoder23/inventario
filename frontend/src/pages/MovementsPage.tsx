import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Search, Download } from 'lucide-react';
import { api } from '../api/client';
import type { Movement, MovementType, Page } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useAsync, useDebounced } from '../lib/useAsync';
import { MOVEMENT_LABEL, fmtDateTime, fmtDelta, fmtNumber, userTimeZone } from '../lib/format';
import { Alert, Card, Empty, Loading, MovementBadge, PageHeader, Pagination } from '../components/ui';
import { MovementForm } from '../components/MovementForm';

export function MovementsPage() {
  const { can } = useAuth();
  const [search, setSearch] = useState('');
  const q = useDebounced(search);
  const [type, setType] = useState<MovementType | ''>('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const [flash, setFlash] = useState<string | null>(null);

  const filters = { q, type, from, to, tz: userTimeZone() };
  const { data, error, loading, reload } = useAsync(
    () => api.get<Page<Movement>>('/api/stock-movements', { ...filters, page, size: 15 }),
    [q, type, from, to, page]);

  const reset = <T,>(setter: (v: T) => void) => (v: T) => { setter(v); setPage(0); };

  return (
    <>
      <PageHeader title="Movimientos" subtitle="Historial auditable de entradas, salidas y ajustes"
        actions={<>
          <button className="btn btn-ghost" onClick={() => api.download('/api/reports/movements/export', filters, 'movimientos.csv')}>
            <Download size={16} /> CSV
          </button>
          {can('ADMIN', 'WAREHOUSE') &&
            <button className="btn btn-primary" onClick={() => setShowForm(true)}><Plus size={16} /> Registrar movimiento</button>}
        </>} />

      {flash && <Alert tone="success">{flash}</Alert>}

      <Card>
        <div className="filters">
          <div className="search">
            <Search size={16} />
            <input placeholder="Producto, SKU, motivo o referencia" value={search}
              onChange={(e) => reset(setSearch)(e.target.value)} aria-label="Buscar" />
          </div>
          <select value={type} onChange={(e) => reset(setType)(e.target.value as MovementType | '')} aria-label="Tipo">
            <option value="">Todos los tipos</option>
            {(Object.keys(MOVEMENT_LABEL) as MovementType[]).map((t) => <option key={t} value={t}>{MOVEMENT_LABEL[t]}</option>)}
          </select>
          <label className="date-field">Desde <input type="date" value={from} max={to || undefined}
            onChange={(e) => reset(setFrom)(e.target.value)} /></label>
          <label className="date-field">Hasta <input type="date" value={to} min={from || undefined}
            onChange={(e) => reset(setTo)(e.target.value)} /></label>
        </div>

        {error && <Alert>{error}</Alert>}
        {loading && !data ? <Loading /> : data && data.content.length === 0 ? (
          <Empty>No hay movimientos con estos filtros</Empty>
        ) : data && (
          <>
            <div className={`table-wrap ${loading ? 'is-loading' : ''}`}>
              <table className="table">
                <thead><tr><th>Fecha</th><th>Producto</th><th>Tipo</th><th className="num">Cantidad</th>
                  <th className="num">Stock</th><th>Motivo</th><th>Referencia</th><th>Usuario</th></tr></thead>
                <tbody>
                  {data.content.map((m) => (
                    <tr key={m.id}>
                      <td className="nowrap muted">{fmtDateTime(m.createdAt)}</td>
                      <td><Link to={`/productos/${m.productId}`} className="link-plain">{m.productName}</Link>
                        <div className="mono small muted">{m.productSku}</div></td>
                      <td><MovementBadge type={m.type} delta={m.delta} /></td>
                      <td className={`num ${m.delta > 0 ? 'text-green' : 'text-red'}`}>{fmtDelta(m.delta)}</td>
                      <td className="num nowrap"><span className="muted">{fmtNumber(m.stockBefore)} → </span><strong>{fmtNumber(m.stockAfter)}</strong></td>
                      <td>{m.reason ?? '—'}</td>
                      <td className="mono small">{m.reference ?? '—'}</td>
                      <td>{m.userName}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements}
              size={data.size} onPage={setPage} />
          </>
        )}
      </Card>

      {showForm && (
        <MovementForm onClose={() => setShowForm(false)} onSaved={(m) => {
          setShowForm(false);
          setFlash(`Movimiento registrado (${MOVEMENT_LABEL[m.type].toLowerCase()}) en ${m.productSku}: stock ${fmtNumber(m.stockBefore)} → ${fmtNumber(m.stockAfter)}`);
          if (page === 0) reload(); else setPage(0);
        }} />
      )}
    </>
  );
}
