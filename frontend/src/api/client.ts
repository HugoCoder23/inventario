import type { ApiErrorBody } from './types';

const BASE = import.meta.env.VITE_API_URL ?? '';
const TOKEN_KEY = 'inventario.token';

export class ApiError extends Error {
  status: number;
  code?: string;
  fieldErrors: Record<string, string>;

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.status = body.status;
    this.code = body.code;
    this.fieldErrors = Object.fromEntries((body.fieldErrors ?? []).map((f) => [f.field, f.message]));
  }
}

export const tokenStore = {
  get(): string | null {
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
  },
  set(token: string | null) {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token);
      else localStorage.removeItem(TOKEN_KEY);
    } catch { /* almacenamiento no disponible */ }
  },
};

let onUnauthorized: () => void = () => {};
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

export type Query = Record<string, string | number | boolean | null | undefined>;

export function toQuery(params: Query = {}): string {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') qs.set(k, String(v));
  });
  const s = qs.toString();
  return s ? `?${s}` : '';
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  const token = tokenStore.get();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let res: Response;
  try {
    res = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError({ status: 0, code: 'NETWORK', message: 'No se pudo conectar con el servidor' });
  }

  if (res.status === 401 && !path.startsWith('/api/auth/login')) onUnauthorized();
  if (!res.ok) {
    let err: ApiErrorBody;
    try {
      err = await res.json();
    } catch {
      // Respuesta sin cuerpo JSON: típicamente el proxy no alcanza al backend.
      err = {
        status: res.status,
        message: res.status >= 500
          ? 'El servidor no está disponible. Intenta de nuevo en unos segundos'
          : `Error ${res.status}`,
      };
    }
    throw new ApiError({ ...err, status: res.status });
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  get: <T>(path: string, params?: Query) => request<T>('GET', path + toQuery(params)),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body),
  del: <T>(path: string) => request<T>('DELETE', path),

  /** Descarga un archivo autenticado (p. ej. exportación CSV). */
  async download(path: string, params: Query, fallbackName: string) {
    const token = tokenStore.get();
    const res = await fetch(BASE + path + toQuery(params), {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!res.ok) throw new ApiError({ status: res.status, message: 'No se pudo generar la exportación' });
    const blob = await res.blob();
    const name = /filename="?([^";]+)"?/.exec(res.headers.get('Content-Disposition') ?? '')?.[1] ?? fallbackName;
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  },
};
