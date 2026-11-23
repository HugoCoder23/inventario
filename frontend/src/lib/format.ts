import type { MovementType, Role } from '../api/types';

const money = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' });
const number = new Intl.NumberFormat('es-MX');
const dateTime = new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeStyle: 'short' });
const dateOnly = new Intl.DateTimeFormat('es-MX', { day: '2-digit', month: 'short' });

export const fmtMoney = (v: number | null | undefined) => money.format(v ?? 0);
export const fmtNumber = (v: number | null | undefined) => number.format(v ?? 0);
export const fmtDateTime = (iso: string) => dateTime.format(new Date(iso));
/** Fecha yyyy-MM-dd sin desfasar por zona horaria. */
export const fmtDay = (ymd: string) => {
  const [y, m, d] = ymd.split('-').map(Number);
  return dateOnly.format(new Date(y, m - 1, d));
};

export const userTimeZone = () => Intl.DateTimeFormat().resolvedOptions().timeZone;

/** yyyy-MM-dd en la zona local. */
export const isoDay = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

export const daysAgo = (n: number) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return isoDay(d);
};

export const MOVEMENT_LABEL: Record<MovementType, string> = {
  IN: 'Entrada',
  OUT: 'Salida',
  ADJUSTMENT: 'Ajuste',
};

export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: 'Administrador',
  WAREHOUSE: 'Encargado de almacén',
  VIEWER: 'Consulta',
};

export const fmtDelta = (d: number) => (d > 0 ? `+${fmtNumber(d)}` : fmtNumber(d));
