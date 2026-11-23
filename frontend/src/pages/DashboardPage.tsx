import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Package, AlertTriangle, DollarSign, Layers, Plus, ArrowRight } from 'lucide-react';
import { api } from '../api/client';
import type { Dashboard } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useAsync } from '../lib/useAsync';
import { fmtDateTime, fmtDelta, fmtMoney, fmtNumber } from '../lib/format';
import { Alert, Card, Empty, Loading, MovementBadge, PageHeader, StatCard, StockBadge } from '../components/ui';
import { MovementForm } from '../components/MovementForm';

export function DashboardPage() {
  const { can } = useAuth();
  const { data, error, loading, reload } = useAsync(() => api.get<Dashboard>('/api/dashboard'), []);
  const [showForm, setShowForm] = useState(false);

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Resumen del inventario en tiempo real"
        actions={can('ADMIN', 'WAREHOUSE') && (
          <button className="btn btn-primary" onClick={() => setShowForm(true)}><Plus size={16} /> Movimiento</button>
        )} />
      {error && <Alert>{error}</Alert>}
      {loading && !data && <Loading />}
      {data && (
        <>
          <div className="stats">
            <StatCard icon={<Package size={20} />} label="Productos activos" value={fmtNumber(data.totalProducts)}
              hint={`${fmtNumber(data.totalUnits)} unidades`} />
            <StatCard icon={<AlertTriangle size={20} />} tone="amber" label="Con stock bajo" value={fmtNumber(data.lowStockCount)}
              hint="En o por debajo del mínimo" />
            <StatCard icon={<DollarSign size={20} />} tone="green" label="Valor a costo" value={fmtMoney(data.inventoryCostValue)}
              hint={`${fmtMoney(data.inventoryRetailValue)} a precio de venta`} />
            <StatCard icon={<Layers size={20} />} tone="violet" label="Categorías" value={fmtNumber(data.categoryCount)} />
          </div>

          <div className="grid-dashboard">
            <Card title="Movimientos recientes"
              actions={<Link to="/movimientos" className="link">Ver todos <ArrowRight size={14} /></Link>}>
              {data.recentMovements.length === 0 ? <Empty>Aún no hay movimientos</Empty> : (
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>Fecha</th><th>Producto</th><th>Tipo</th><th className="num">Cantidad</th><th className="num">Stock</th></tr></thead>
                    <tbody>
                      {data.recentMovements.map((m) => (
                        <tr key={m.id}>
                          <td className="nowrap muted">{fmtDateTime(m.createdAt)}</td>
                          <td><Link to={`/productos/${m.productId}`} className="link-plain">{m.productName}</Link>
                            <div className="mono small muted">{m.productSku}</div></td>
                          <td><MovementBadge type={m.type} delta={m.delta} /></td>
                          <td className={`num ${m.delta > 0 ? 'text-green' : 'text-red'}`}>{fmtDelta(m.delta)}</td>
                          <td className="num">{fmtNumber(m.stockAfter)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>

            <Card title="Alertas de stock bajo"
              actions={<Link to="/productos?lowStock=true" className="link">Ver todos <ArrowRight size={14} /></Link>}>
              {data.lowStockProducts.length === 0 ? <Empty>Todo el inventario está por encima del mínimo</Empty> : (
                <ul className="alert-list">
                  {data.lowStockProducts.map((p) => (
                    <li key={p.id}>
                      <Link to={`/productos/${p.id}`} className="link-plain">
                        <div className="alert-item-name">{p.name}</div>
                        <div className="mono small muted">{p.sku}</div>
                      </Link>
                      <div className="alert-item-stock">
                        <strong>{fmtNumber(p.stock)}</strong><span className="muted small"> / mín {p.minStock}</span>
                        <StockBadge stock={p.stock} min={p.minStock} />
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>
        </>
      )}
      {showForm && (
        <MovementForm onClose={() => setShowForm(false)} onSaved={() => { setShowForm(false); reload(); }} />
      )}
    </>
  );
}
