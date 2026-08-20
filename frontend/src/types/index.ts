export type Role = "admin" | "warehouse_worker" | "warehouse_manager" | "inspector" | "cashier";

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
  /** Tenant scope — from GET /users/me and auth login responses */
  ecosystemId?: string;
  ecosystemName?: string;
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
  provider?: string;
  recipient?: string;
  userId: string;
  timestamp: string;
  note?: string;
  /** Present on API list/detail responses */
  productName?: string;
  userName?: string;
}

export interface StockMovementInput {
  type: StockMovementType;
  productId: string;
  qty: number;
  fromWarehouseId?: string | null;
  toWarehouseId?: string | null;
  provider?: string | null;
  recipient?: string | null;
  note?: string | null;
}

export type WarehouseInput = Omit<Warehouse, "id">;

export interface UserProvisionInput {
  name: string;
  email: string;
  password: string;
  role: Role;
  active: boolean;
}

export interface UserUpdateInput {
  role?: Role;
  active?: boolean;
}

export interface AuditEntry {
  id: string;
  userId?: string | null;
  userName?: string | null;
  action: string;
  entity: string;
  entityId: string;
  entityLabel?: string | null;
  timestamp: string;
  details?: string | null;
}

export interface CartItem {
  productId: string;
  name: string;
  sku: string;
  qty: number;
  unitPrice: number;
  warehouseId: string;
  warehouseName?: string;
  /** Present while building a cart in the cashier UI. */
  maxStock?: number;
}

export function cartLineKey(productId: string, warehouseId: string) {
  return `${productId}:${warehouseId}`;
}

export type TransactionStatus = "completed" | "refunded" | "void";

export interface Transaction {
  id: string;
  items: CartItem[];
  subtotal: number;
  tax: number;
  total: number;
  cashierId: string;
  cashierName?: string;
  status: TransactionStatus;
  timestamp: string;
}

export interface TransactionCreateInput {
  items: CartItem[];
  subtotal: number;
  tax: number;
  total: number;
}

export type BusinessModePreference = "auto" | "single" | "multi";

export interface WorkspaceSettings {
  businessName: string;
  contactEmail: string;
  taxRate: number;
  taxLabel: string;
  receiptFooter: string | null;
  receiptLogoUrl: string | null;
  businessMode: BusinessModePreference;
  updatedAt: string;
}

export type WorkspaceSettingsUpdate = Partial<
  Pick<
    WorkspaceSettings,
    | "businessName"
    | "contactEmail"
    | "taxRate"
    | "taxLabel"
    | "receiptFooter"
    | "receiptLogoUrl"
    | "businessMode"
  >
>;
