export type Role = 'ADMIN' | 'WAREHOUSE' | 'VIEWER';
export type MovementType = 'IN' | 'OUT' | 'ADJUSTMENT';
export type AdjustmentDirection = 'INCREASE' | 'DECREASE';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  productCount: number;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description?: string;
  categoryId: number;
  categoryName: string;
  cost: number;
  price: number;
  minStock: number;
  stock: number;
  lowStock: boolean;
  active: boolean;
  stockValue: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductInput {
  sku: string;
  name: string;
  description?: string;
  categoryId: number | null;
  cost: number | null;
  price: number | null;
  minStock: number | null;
  initialStock?: number | null;
}

export interface Movement {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  type: MovementType;
  quantity: number;
  delta: number;
  stockBefore: number;
  stockAfter: number;
  reason?: string;
  reference?: string;
  userId: number;
  userName: string;
  createdAt: string;
}

export interface MovementInput {
  productId: number | null;
  type: MovementType;
  quantity: number | null;
  direction?: AdjustmentDirection;
  reason?: string;
  reference?: string;
}

export interface Dashboard {
  totalProducts: number;
  totalUnits: number;
  inventoryCostValue: number;
  inventoryRetailValue: number;
  lowStockCount: number;
  categoryCount: number;
  lowStockProducts: Product[];
  recentMovements: Movement[];
}

export interface MovementSummary {
  from: string;
  to: string;
  totalMovements: number;
  unitsIn: number;
  unitsOut: number;
  netChange: number;
  byType: { type: MovementType; movements: number; units: number; netChange: number }[];
  byProduct: {
    productId: number; sku: string; name: string; movements: number;
    unitsIn: number; unitsOut: number; netChange: number;
  }[];
  byDay: { date: string; unitsIn: number; unitsOut: number }[];
}

export interface ApiErrorBody {
  status: number;
  code?: string;
  message: string;
  fieldErrors?: { field: string; message: string }[];
}
