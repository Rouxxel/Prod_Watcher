import type {
  AuditEntry,
  Product,
  StockMovement,
  Transaction,
  User,
  Warehouse,
} from "@/types";

export const users: User[] = [
  { id: "u1", name: "Alex Reyes", email: "alex@acme.co", role: "admin", active: true },
  { id: "u2", name: "Maya Chen", email: "maya@acme.co", role: "warehouse_manager", active: true },
  { id: "u3", name: "Jordan Park", email: "jordan@acme.co", role: "warehouse_worker", active: true },
  { id: "u4", name: "Sam Holt", email: "sam@acme.co", role: "inspector", active: true },
  { id: "u5", name: "Riley Vega", email: "riley@acme.co", role: "cashier", active: true },
  { id: "u6", name: "Devon Cruz", email: "devon@acme.co", role: "cashier", active: false },
];

export const warehouses: Warehouse[] = [
  { id: "w1", name: "Central Depot", location: "Caracas, VE" },
  { id: "w2", name: "North Hub", location: "Valencia, VE" },
  { id: "w3", name: "Coastal Annex", location: "Maracaibo, VE" },
];

const img = (seed: string, n = 3) =>
  Array.from({ length: n }, (_, i) => `https://picsum.photos/seed/${seed}-${i + 1}/600/600`);

export const products: Product[] = [
  { id: "p1", name: "Ceramic Pour-Over Kettle", sku: "KTL-001", category: "Kitchen", price: 78.0, stock: 42, warehouseId: "w1", lowStockThreshold: 10, images: img("kettle", 4) },
  { id: "p2", name: "Walnut Cutting Board", sku: "WCB-220", category: "Kitchen", price: 54.5, stock: 6, warehouseId: "w1", lowStockThreshold: 10, images: img("board", 3) },
  { id: "p3", name: "Linen Apron — Charcoal", sku: "APR-CHR", category: "Apparel", price: 39.0, stock: 0, warehouseId: "w2", lowStockThreshold: 5, images: img("apron", 2) },
  { id: "p4", name: "Brass Espresso Tamper", sku: "TMP-58", category: "Barista", price: 62.0, stock: 24, warehouseId: "w1", lowStockThreshold: 8, images: img("tamper", 3) },
  { id: "p5", name: "Stoneware Mug Set (4)", sku: "MUG-S4", category: "Kitchen", price: 48.0, stock: 88, warehouseId: "w3", lowStockThreshold: 15, images: img("mugs", 4) },
  { id: "p6", name: "Cold Brew Carafe — 1L", sku: "CBC-1L", category: "Barista", price: 34.0, stock: 4, warehouseId: "w2", lowStockThreshold: 6, images: img("carafe", 2) },
  { id: "p7", name: "Cast Iron Skillet 10\"", sku: "CIS-10", category: "Kitchen", price: 92.0, stock: 17, warehouseId: "w1", lowStockThreshold: 5, images: img("skillet", 3) },
  { id: "p8", name: "Bamboo Tea Tray", sku: "TEA-BMB", category: "Kitchen", price: 28.0, stock: 31, warehouseId: "w3", lowStockThreshold: 10, images: img("tray", 2) },
  { id: "p9", name: "Hand-Thrown Vase", sku: "VAS-HT1", category: "Home", price: 120.0, stock: 9, warehouseId: "w2", lowStockThreshold: 4, images: img("vase", 4) },
  { id: "p10", name: "Wool Throw Blanket", sku: "WTB-77", category: "Home", price: 145.0, stock: 12, warehouseId: "w3", lowStockThreshold: 4, images: img("blanket", 3) },
  { id: "p11", name: "Beeswax Candle Trio", sku: "CDL-BW3", category: "Home", price: 36.0, stock: 60, warehouseId: "w1", lowStockThreshold: 10, images: img("candle", 3) },
  { id: "p12", name: "Stainless Milk Pitcher", sku: "MLK-350", category: "Barista", price: 22.0, stock: 2, warehouseId: "w2", lowStockThreshold: 6, images: img("pitcher", 1) },
];

const iso = (daysAgo: number, h = 9) => {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  d.setHours(h, 12, 0, 0);
  return d.toISOString();
};

export const stockMovements: StockMovement[] = [
  { id: "m1", type: "IN", productId: "p1", qty: 20, toWarehouseId: "w1", userId: "u3", timestamp: iso(0, 8), note: "Supplier delivery" },
  { id: "m2", type: "OUT", productId: "p2", qty: 4, fromWarehouseId: "w1", userId: "u5", timestamp: iso(0, 10), note: "POS sale" },
  { id: "m3", type: "TRANSFER", productId: "p5", qty: 12, fromWarehouseId: "w3", toWarehouseId: "w1", userId: "u2", timestamp: iso(1, 14) },
  { id: "m4", type: "ADJUSTMENT", productId: "p6", qty: -2, fromWarehouseId: "w2", userId: "u4", timestamp: iso(1, 16), note: "Damaged units" },
  { id: "m5", type: "IN", productId: "p10", qty: 8, toWarehouseId: "w3", userId: "u3", timestamp: iso(2, 9) },
  { id: "m6", type: "OUT", productId: "p4", qty: 3, fromWarehouseId: "w1", userId: "u5", timestamp: iso(2, 11) },
  { id: "m7", type: "TRANSFER", productId: "p11", qty: 20, fromWarehouseId: "w1", toWarehouseId: "w2", userId: "u2", timestamp: iso(3, 13) },
  { id: "m8", type: "ADJUSTMENT", productId: "p12", qty: 1, fromWarehouseId: "w2", userId: "u4", timestamp: iso(4, 10), note: "Recount" },
];

export const auditEntries: AuditEntry[] = [
  { id: "a1", userId: "u1", action: "USER_CREATED", entity: "user", entityId: "u6", timestamp: iso(0, 7), details: "Invited Devon Cruz" },
  { id: "a2", userId: "u2", action: "PRODUCT_UPDATED", entity: "product", entityId: "p2", timestamp: iso(0, 9), details: "Price 52.00 → 54.50" },
  { id: "a3", userId: "u4", action: "AUDIT_RUN", entity: "warehouse", entityId: "w2", timestamp: iso(1, 16), details: "Spot check, 2 variances" },
  { id: "a4", userId: "u3", action: "STOCK_RECEIVED", entity: "movement", entityId: "m1", timestamp: iso(0, 8) },
  { id: "a5", userId: "u5", action: "TRANSACTION_COMPLETED", entity: "transaction", entityId: "t1", timestamp: iso(0, 10) },
  { id: "a6", userId: "u1", action: "ROLE_CHANGED", entity: "user", entityId: "u3", timestamp: iso(5, 11), details: "worker → manager (reverted)" },
];

export const transactions: Transaction[] = [
  {
    id: "t1",
    items: [
      { productId: "p2", name: "Walnut Cutting Board", sku: "WCB-220", qty: 2, unitPrice: 54.5 },
      { productId: "p11", name: "Beeswax Candle Trio", sku: "CDL-BW3", qty: 1, unitPrice: 36.0 },
    ],
    subtotal: 145.0,
    tax: 23.2,
    total: 168.2,
    cashierId: "u5",
    status: "completed",
    timestamp: iso(0, 10),
  },
  {
    id: "t2",
    items: [{ productId: "p4", name: "Brass Espresso Tamper", sku: "TMP-58", qty: 1, unitPrice: 62.0 }],
    subtotal: 62.0,
    tax: 9.92,
    total: 71.92,
    cashierId: "u5",
    status: "completed",
    timestamp: iso(1, 12),
  },
  {
    id: "t3",
    items: [{ productId: "p1", name: "Ceramic Pour-Over Kettle", sku: "KTL-001", qty: 1, unitPrice: 78.0 }],
    subtotal: 78.0,
    tax: 12.48,
    total: 90.48,
    cashierId: "u5",
    status: "refunded",
    timestamp: iso(2, 15),
  },
  {
    id: "t4",
    items: [
      { productId: "p5", name: "Stoneware Mug Set (4)", sku: "MUG-S4", qty: 3, unitPrice: 48.0 },
      { productId: "p8", name: "Bamboo Tea Tray", sku: "TEA-BMB", qty: 1, unitPrice: 28.0 },
    ],
    subtotal: 172.0,
    tax: 27.52,
    total: 199.52,
    cashierId: "u5",
    status: "completed",
    timestamp: iso(3, 11),
  },
  {
    id: "t5",
    items: [{ productId: "p10", name: "Wool Throw Blanket", sku: "WTB-77", qty: 1, unitPrice: 145.0 }],
    subtotal: 145.0,
    tax: 23.2,
    total: 168.2,
    cashierId: "u6",
    status: "void",
    timestamp: iso(4, 14),
  },
];
