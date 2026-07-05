export type Role =
  | "admin"
  | "warehouse_worker"
  | "warehouse_manager"
  | "inspector"
  | "cashier";

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
}

export interface Warehouse {
  id: string;
  name: string;
  location: string;
}

export interface Product {
  id: string;
  name: string;
  sku: string;
  category: string;
  price: number;
  stock: number;
  warehouseId: string;
  lowStockThreshold: number;
  images: string[];
}

export type ProductInput = Omit<Product, "id">;

export type StockMovementType = "IN" | "OUT" | "TRANSFER" | "ADJUSTMENT";

export interface StockMovement {
  id: string;
  type: StockMovementType;
  productId: string;
  qty: number;
  fromWarehouseId?: string;
  toWarehouseId?: string;
  userId: string;
  timestamp: string;
  note?: string;
}

export interface AuditEntry {
  id: string;
  userId: string;
  action: string;
  entity: string;
  entityId: string;
  timestamp: string;
  details?: string;
}

export interface CartItem {
  productId: string;
  name: string;
  sku: string;
  qty: number;
  unitPrice: number;
}

export type TransactionStatus = "completed" | "refunded" | "void";

export interface Transaction {
  id: string;
  items: CartItem[];
  subtotal: number;
  tax: number;
  total: number;
  cashierId: string;
  status: TransactionStatus;
  timestamp: string;
}
