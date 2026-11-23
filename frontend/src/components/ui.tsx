import { useEffect, type ReactNode } from 'react';
import { ChevronLeft, ChevronRight, X, ArrowDownLeft, ArrowUpRight, SlidersHorizontal } from 'lucide-react';
import type { MovementType } from '../api/types';
import { MOVEMENT_LABEL, fmtNumber } from '../lib/format';

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

export function Card({ title, actions, children, className = '' }:
  { title?: ReactNode; actions?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="card-header">
          {title && <h2>{title}</h2>}
          {actions}
        </header>
      )}
      {children}
    </section>
  );
}

export function StatCard({ icon, label, value, tone = 'blue', hint }:
  { icon: ReactNode; label: string; value: string; tone?: 'blue' | 'green' | 'amber' | 'violet'; hint?: string }) {
  return (
    <div className="stat">
      <div className={`stat-icon tone-${tone}`}>{icon}</div>
      <div>
        <div className="stat-value">{value}</div>
        <div className="stat-label">{label}</div>
        {hint && <div className="stat-hint">{hint}</div>}
      </div>
    </div>
  );
}

export function MovementBadge({ type, delta }: { type: MovementType; delta?: number }) {
  const icon = type === 'IN' ? <ArrowDownLeft size={13} />
    : type === 'OUT' ? <ArrowUpRight size={13} /> : <SlidersHorizontal size={13} />;
  const tone = type === 'IN' ? 'green' : type === 'OUT' ? 'red' : 'amber';
  return (
    <span className={`badge badge-${tone}`}>
      {icon}{MOVEMENT_LABEL[type]}{type === 'ADJUSTMENT' && delta !== undefined ? (delta > 0 ? ' +' : ' −') : ''}
    </span>
  );
}

export function StockBadge({ stock, min, active = true }: { stock: number; min: number; active?: boolean }) {
  if (!active) return <span className="badge badge-gray">Inactivo</span>;
  if (stock === 0) return <span className="badge badge-red">Agotado</span>;
  if (stock <= min) return <span className="badge badge-amber">Stock bajo</span>;
  return <span className="badge badge-green">Disponible</span>;
}

export function Pagination({ page, totalPages, totalElements, size, onPage }:
  { page: number; totalPages: number; totalElements: number; size: number; onPage: (p: number) => void }) {
  if (totalElements === 0) return null;
  const from = page * size + 1;
  const to = Math.min(totalElements, (page + 1) * size);
  return (
    <div className="pagination">
      <span className="muted">{fmtNumber(from)}–{fmtNumber(to)} de {fmtNumber(totalElements)}</span>
      <div className="pagination-buttons">
        <button className="btn btn-ghost btn-icon" disabled={page === 0} onClick={() => onPage(page - 1)}
          aria-label="Página anterior"><ChevronLeft size={16} /></button>
        <span className="page-indicator">{page + 1} / {Math.max(totalPages, 1)}</span>
        <button className="btn btn-ghost btn-icon" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}
          aria-label="Página siguiente"><ChevronRight size={16} /></button>
      </div>
    </div>
  );
}

export function Modal({ title, onClose, children, width = 520 }:
  { title: string; onClose: () => void; children: ReactNode; width?: number }) {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal" style={{ maxWidth: width }} role="dialog" aria-modal="true" aria-label={title}>
        <header className="modal-header">
          <h2>{title}</h2>
          <button className="btn btn-ghost btn-icon" onClick={onClose} aria-label="Cerrar"><X size={18} /></button>
        </header>
        {children}
      </div>
    </div>
  );
}

export function Field({ label, error, hint, children, required }:
  { label: string; error?: string; hint?: string; children: ReactNode; required?: boolean }) {
  return (
    <label className={`field ${error ? 'has-error' : ''}`}>
      <span className="field-label">{label}{required && <span className="req"> *</span>}</span>
      {children}
      {error ? <span className="field-error">{error}</span> : hint ? <span className="field-hint">{hint}</span> : null}
    </label>
  );
}

export function Alert({ tone = 'error', children }: { tone?: 'error' | 'success' | 'info'; children: ReactNode }) {
  return <div className={`alert alert-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>{children}</div>;
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>;
}

export function Loading() {
  return <div className="loading"><span className="spinner" /> Cargando…</div>;
}
