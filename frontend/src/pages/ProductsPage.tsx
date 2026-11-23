import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Plus, Search, Download, Eye, Pencil, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { api } from '../api/client';
import type { Category, Page, Product } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useAsync, useDebounced } from '../lib/useAsync';
import { fmtMoney, fmtNumber } from '../lib/format';
import { Alert, Card, Empty, Loading, PageHeader, Pagination, StockBadge } from '../components/ui';

const COLUMNS: { key: string; label: string; num?: boolean }[] = [
  { key: 'sku', label: 'SKU' },
  { key: 'name', label: 'Producto' },
  { key: 'category', label: 'Categoría' },
  { key: 'price', label: 'Precio', num: true },
  { key: 'stock', label: 'Stock', num: true },
];

export function ProductsPage() {
  const { can } = useAuth();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const [search, setSearch] = useState(params.get('q') ?? '');
  const q = useDebounced(search);

  const categoryId = params.get('categoryId') ?? '';
  const status = params.get('status') ?? 'active';
  const lowStock = params.get('lowStock') === 'true';
  const sort = params.get('sort') ?? 'name,asc';
  const page = Number(params.get('page') ?? 0);
  const size = 10;

  const update = (changes: Record<string, string | null>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => (v === null || v === '' ? next.delete(k) : next.set(k, v)));
    if (!('page' in changes)) next.delete('page');
    setParams(next, { replace: true });
  };

  const categories = useAsync(() => api.get<Category[]>('/api/categories'), []);
  const query = {
    q, categoryId, sort, page, size,
    active: status === 'all' ? undefined : status === 'active',
    lowStock: lowStock || undefined,
  };
  const { data, error, loading } = useAsync(
    () => api.get<Page<Product>>('/api/products', query),
    [q, categoryId, status, lowStock, sort, page]);

  const [sortKey, sortDir] = sort.split(',');
  const toggleSort = (key: string) =>
    update({ sort: `${key},${sortKey === key && sortDir === 'asc' ? 'desc' : 'asc'}` });

  return (
    <>
      <PageHeader title="Productos" subtitle="Catálogo, existencias y alertas"
        actions={<>
          <button className="btn btn-ghost" onClick={() => api.download('/api/reports/products/export',
            { q, categoryId, active: query.active, lowStock: query.lowStock }, 'productos.csv')}>
            <Download size={16} /> CSV
          </button>
          {can('ADMIN', 'WAREHOUSE') &&
            <Link to="/productos/nuevo" className="btn btn-primary"><Plus size={16} /> Nuevo producto</Link>}
        </>} />

      <Card>
        <div className="filters">
          <div className="search">
            <Search size={16} />
            <input placeholder="Buscar por SKU, nombre o descripción" value={search}
              onChange={(e) => { setSearch(e.target.value); update({ q: e.target.value }); }} aria-label="Buscar" />
          </div>
          <select value={categoryId} onChange={(e) => update({ categoryId: e.target.value })} aria-label="Categoría">
            <option value="">Todas las categorías</option>
            {categories.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select value={status} onChange={(e) => update({ status: e.target.value })} aria-label="Estado">
            <option value="active">Activos</option>
            <option value="inactive">Inactivos</option>
            <option value="all">Todos</option>
          </select>
          <label className="check">
            <input type="checkbox" checked={lowStock} onChange={(e) => update({ lowStock: e.target.checked ? 'true' : null })} />
            Solo stock bajo
          </label>
        </div>

        {error && <Alert>{error}</Alert>}
        {loading && !data ? <Loading /> : data && data.content.length === 0 ? (
          <Empty>No hay productos que coincidan con los filtros</Empty>
        ) : data && (
          <>
            <div className={`table-wrap ${loading ? 'is-loading' : ''}`}>
              <table className="table table-hover">
                <thead>
                  <tr>
                    {COLUMNS.map((c) => (
                      <th key={c.key} className={c.num ? 'num' : ''}>
                        <button className="th-sort" onClick={() => toggleSort(c.key)}>
                          {c.label}
                          {sortKey === c.key ? (sortDir === 'asc' ? <ArrowUp size={13} /> : <ArrowDown size={13} />)
                            : <ArrowUpDown size={13} className="muted" />}
                        </button>
                      </th>
                    ))}
                    <th>Estado</th>
                    <th className="actions-col">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((p) => (
                    <tr key={p.id} onClick={() => navigate(`/productos/${p.id}`)}>
                      <td className="mono">{p.sku}</td>
                      <td className="strong">{p.name}</td>
                      <td>{p.categoryName}</td>
                      <td className="num">{fmtMoney(p.price)}</td>
                      <td className="num"><strong>{fmtNumber(p.stock)}</strong><span className="muted small"> / {p.minStock}</span></td>
                      <td><StockBadge stock={p.stock} min={p.minStock} active={p.active} /></td>
                      <td className="actions-col" onClick={(e) => e.stopPropagation()}>
                        <Link to={`/productos/${p.id}`} className="btn btn-ghost btn-icon" title="Ver detalle"><Eye size={16} /></Link>
                        {can('ADMIN', 'WAREHOUSE') &&
                          <Link to={`/productos/${p.id}/editar`} className="btn btn-ghost btn-icon" title="Editar"><Pencil size={16} /></Link>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements}
              size={data.size} onPage={(p) => update({ page: String(p) })} />
          </>
        )}
      </Card>
    </>
  );
}
