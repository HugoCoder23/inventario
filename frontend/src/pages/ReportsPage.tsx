import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Download, ArrowDownLeft, ArrowUpRight, Activity, Scale } from 'lucide-react';
import { api } from '../api/client';
import type { MovementSummary, MovementType, Page, Product } from '../api/types';
import { useAsync } from '../lib/useAsync';
import { MOVEMENT_LABEL, daysAgo, fmtDay, fmtDelta, fmtMoney, fmtNumber, isoDay, userTimeZone } from '../lib/format';
import { Alert, Card, Empty, Loading, MovementBadge, PageHeader, StatCard, StockBadge } from '../components/ui';

const PRESETS = [
  { label: '7 días', days: 6 },
  { label: '30 días', days: 29 },
  { label: '90 días', days: 89 },
];

export function ReportsPage() {
  const [from, setFrom] = useState(daysAgo(29));
  const [to, setTo] = useState(isoDay(new Date()));
  const [type, setType] = useState<MovementType | ''>('');
  const [productId, setProductId] = useState('');
  const tz = userTimeZone();

  const products = useAsync(() => api.get<Page<Product>>('/api/products', { size: 100, sort: 'name,asc' }), []);
  const summary = useAsync(
    () => api.get<MovementSummary>('/api/reports/movements', { from, to, tz, type, productId }),
    [from, to, type, productId]);
  const lowStock = useAsync(() => api.get<Product[]>('/api/reports/low-stock'), []);

  const s = summary.data;

  return (
    <>
      <PageHeader title="Reportes" subtitle="Movimientos por periodo, producto y tipo"
        actions={<button className="btn btn-ghost" onClick={() => api.download('/api/reports/movements/export',
          { from, to, tz, type, productId }, 'movimientos.csv')}><Download size={16} /> Exportar movimientos</button>} />

      <Card>
        <div className="filters">
          <div className="presets">
            {PRESETS.map((p) => (
              <button key={p.label} className={`btn btn-sm ${from === daysAgo(p.days) && to === isoDay(new Date()) ? 'btn-soft' : 'btn-ghost'}`}
                onClick={() => { setFrom(daysAgo(p.days)); setTo(isoDay(new Date())); }}>{p.label}</button>
            ))}
          </div>
          <label className="date-field">Desde <input type="date" value={from} max={to} onChange={(e) => e.target.value && setFrom(e.target.value)} /></label>
          <label className="date-field">Hasta <input type="date" value={to} min={from} onChange={(e) => e.target.value && setTo(e.target.value)} /></label>
          <select value={type} onChange={(e) => setType(e.target.value as MovementType | '')} aria-label="Tipo">
            <option value="">Todos los tipos</option>
            {(Object.keys(MOVEMENT_LABEL) as MovementType[]).map((t) => <option key={t} value={t}>{MOVEMENT_LABEL[t]}</option>)}
          </select>
          <select value={productId} onChange={(e) => setProductId(e.target.value)} aria-label="Producto">
            <option value="">Todos los productos</option>
            {products.data?.content.map((p) => <option key={p.id} value={p.id}>{p.sku} · {p.name}</option>)}
          </select>
        </div>
      </Card>

      {summary.error && <Alert>{summary.error}</Alert>}
      {summary.loading && !s && <Loading />}
      {s && (
        <>
          <div className="stats">
            <StatCard icon={<Activity size={20} />} label="Movimientos" value={fmtNumber(s.totalMovements)} />
            <StatCard icon={<ArrowDownLeft size={20} />} tone="green" label="Unidades que entraron" value={fmtNumber(s.unitsIn)} />
            <StatCard icon={<ArrowUpRight size={20} />} tone="amber" label="Unidades que salieron" value={fmtNumber(s.unitsOut)} />
            <StatCard icon={<Scale size={20} />} tone="violet" label="Variación neta" value={fmtDelta(s.netChange)} />
          </div>

          <Card title="Entradas y salidas por día">
            <DailyChart data={s.byDay} />
          </Card>

          <div className="grid-dashboard">
            <Card title="Por producto">
              {s.byProduct.length === 0 ? <Empty>Sin movimientos en el periodo</Empty> : (
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>Producto</th><th className="num">Movs.</th><th className="num">Entradas</th>
                      <th className="num">Salidas</th><th className="num">Neto</th></tr></thead>
                    <tbody>
                      {s.byProduct.slice(0, 15).map((p) => (
                        <tr key={p.productId}>
                          <td><Link to={`/productos/${p.productId}`} className="link-plain">{p.name}</Link>
                            <div className="mono small muted">{p.sku}</div></td>
                          <td className="num">{fmtNumber(p.movements)}</td>
                          <td className="num text-green">{fmtNumber(p.unitsIn)}</td>
                          <td className="num text-red">{fmtNumber(p.unitsOut)}</td>
                          <td className="num strong">{fmtDelta(p.netChange)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>
            <Card title="Por tipo">
              {s.byType.length === 0 ? <Empty>Sin movimientos</Empty> : (
                <ul className="type-list">
                  {s.byType.map((t) => (
                    <li key={t.type}>
                      <MovementBadge type={t.type} />
                      <span>{fmtNumber(t.movements)} movs.</span>
                      <strong>{fmtNumber(t.units)} u.</strong>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>
        </>
      )}

      <Card title="Productos bajo el mínimo"
        actions={<button className="btn btn-ghost btn-sm" onClick={() => api.download('/api/reports/products/export',
          { lowStock: true, active: true }, 'stock-bajo.csv')}><Download size={14} /> CSV</button>}>
        {lowStock.error && <Alert>{lowStock.error}</Alert>}
        {lowStock.data && lowStock.data.length === 0 && <Empty>No hay productos con stock bajo</Empty>}
        {lowStock.data && lowStock.data.length > 0 && (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>SKU</th><th>Producto</th><th>Categoría</th><th className="num">Stock</th>
                <th className="num">Mínimo</th><th className="num">Faltan</th><th className="num">Costo reposición</th><th>Estado</th></tr></thead>
              <tbody>
                {lowStock.data.map((p) => {
                  const missing = Math.max(0, p.minStock - p.stock + 1);
                  return (
                    <tr key={p.id}>
                      <td className="mono">{p.sku}</td>
                      <td><Link to={`/productos/${p.id}`} className="link-plain">{p.name}</Link></td>
                      <td>{p.categoryName}</td>
                      <td className="num strong">{fmtNumber(p.stock)}</td>
                      <td className="num">{fmtNumber(p.minStock)}</td>
                      <td className="num">{fmtNumber(missing)}</td>
                      <td className="num">{fmtMoney(missing * p.cost)}</td>
                      <td><StockBadge stock={p.stock} min={p.minStock} /></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}

/** Gráfica de barras agrupadas (entradas/salidas) en SVG, sin dependencias. */
function DailyChart({ data }: { data: MovementSummary['byDay'] }) {
  const [hover, setHover] = useState<number | null>(null);
  const max = Math.max(1, ...data.map((d) => Math.max(d.unitsIn, d.unitsOut)));
  const W = 900, H = 220, PAD_L = 40, PAD_B = 26, PAD_T = 10;
  const innerW = W - PAD_L - 8, innerH = H - PAD_B - PAD_T;
  const slot = innerW / Math.max(1, data.length);
  const barW = Math.max(1.5, Math.min(14, slot / 2 - 1));
  const y = (v: number) => PAD_T + innerH - (v / max) * innerH;
  const ticks = [0, 0.5, 1].map((t) => Math.round(max * t));
  const labelEvery = Math.ceil(data.length / 8);
  const h = hover !== null ? data[hover] : null;

  if (data.every((d) => d.unitsIn === 0 && d.unitsOut === 0)) return <Empty>Sin movimientos en el periodo</Empty>;

  return (
    <div className="chart">
      <div className="chart-legend">
        <span><i className="dot dot-in" /> Entradas</span>
        <span><i className="dot dot-out" /> Salidas</span>
        <span className="chart-tip">{h ? `${fmtDay(h.date)}: +${fmtNumber(h.unitsIn)} / −${fmtNumber(h.unitsOut)}` : ' '}</span>
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label="Entradas y salidas por día" preserveAspectRatio="none">
        {ticks.map((t) => (
          <g key={t}>
            <line x1={PAD_L} x2={W - 8} y1={y(t)} y2={y(t)} className="grid-line" />
            <text x={PAD_L - 6} y={y(t) + 4} textAnchor="end" className="axis-label">{fmtNumber(t)}</text>
          </g>
        ))}
        {data.map((d, i) => {
          const x = PAD_L + i * slot + slot / 2;
          return (
            <g key={d.date} onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(null)}>
              <rect x={PAD_L + i * slot} y={PAD_T} width={slot} height={innerH} className={hover === i ? 'hover-band' : 'hit'} />
              <rect x={x - barW - 0.5} y={y(d.unitsIn)} width={barW} height={Math.max(0, PAD_T + innerH - y(d.unitsIn))} rx={2} className="bar-in" />
              <rect x={x + 0.5} y={y(d.unitsOut)} width={barW} height={Math.max(0, PAD_T + innerH - y(d.unitsOut))} rx={2} className="bar-out" />
              {i % labelEvery === 0 && <text x={x} y={H - 8} textAnchor="middle" className="axis-label">{fmtDay(d.date)}</text>}
            </g>
          );
        })}
      </svg>
    </div>
  );
}
