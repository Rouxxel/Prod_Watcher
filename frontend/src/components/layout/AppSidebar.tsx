import { Link, useRouterState } from "@tanstack/react-router";
import logoUrl from "@/assets/prodwatch-logo.png";
import { useAppMode } from "@/hooks/use-app-mode";
import {
  LayoutDashboard,
  Package,
  Warehouse,
  ArrowLeftRight,
  ClipboardCheck,
  ShoppingCart,
  ScanBarcode,
  Receipt,
  Users,
  Settings,
} from "lucide-react";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { RoleBadge } from "./RoleBadge";
import { useCurrentUser } from "@/hooks/use-current-user";
import type { Role } from "@/types";

type Item = { title: string; url: string; icon: typeof Package; roles?: Role[] };

const inventoryItems: Item[] = [
  { title: "Dashboard", url: "/", icon: LayoutDashboard },
  { title: "Products", url: "/products", icon: Package },
  { title: "Warehouses", url: "/warehouses", icon: Warehouse },
  { title: "Stock Movements", url: "/stock-movements", icon: ArrowLeftRight },
  { title: "Inventory Audit", url: "/audit", icon: ClipboardCheck, roles: ["admin", "inspector", "warehouse_manager", "warehouse_worker"] },
];

const sellingItems: Item[] = [
  { title: "Cashier", url: "/cashier", icon: ScanBarcode, roles: ["admin", "cashier", "inspector"] },
  { title: "Cart", url: "/cart", icon: ShoppingCart, roles: ["admin", "cashier"] },
  { title: "Transactions", url: "/transactions", icon: Receipt, roles: ["admin", "cashier", "warehouse_manager", "inspector"] },
];

const adminItems: Item[] = [
  { title: "Users & Roles", url: "/users", icon: Users, roles: ["admin"] },
  { title: "Settings", url: "/settings", icon: Settings, roles: ["admin"] },
];

export function AppSidebar() {
  const { state } = useSidebar();
  const collapsed = state === "collapsed";
  const path = useRouterState({ select: (s) => s.location.pathname });
  const { user } = useCurrentUser();
  const { mode } = useAppMode();

  const visible = (items: Item[]) =>
    items.filter((i) => !i.roles || (user && i.roles.includes(user.role)));

  const renderGroup = (label: string, items: Item[]) => {
    const filtered = visible(items);
    if (filtered.length === 0) return null;
    return (
      <SidebarGroup>
        <SidebarGroupLabel>{label}</SidebarGroupLabel>
        <SidebarGroupContent>
          <SidebarMenu>
            {filtered.map((item) => {
              const active = item.url === "/" ? path === "/" : path.startsWith(item.url);
              return (
                <SidebarMenuItem key={item.url}>
                  <SidebarMenuButton asChild isActive={active} tooltip={item.title}>
                    <Link to={item.url} className="flex items-center gap-2">
                      <item.icon className="h-4 w-4 shrink-0" />
                      {!collapsed && <span>{item.title}</span>}
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              );
            })}
          </SidebarMenu>
        </SidebarGroupContent>
      </SidebarGroup>
    );
  };

  return (
    <Sidebar collapsible="icon" className="border-r border-sidebar-border">
      <SidebarHeader className={collapsed ? "px-1 py-3" : "px-3 py-4"}>
        <div className="flex items-center gap-2">
          <div
            className={`flex shrink-0 items-center justify-center overflow-hidden rounded-md bg-[oklch(0.92_0.01_20)] shadow-[var(--shadow-neon)] ring-1 ring-border ${
              collapsed ? "h-8 w-8 mx-auto" : "h-10 w-10"
            }`}
          >
            <img
              src={logoUrl}
              alt="ProdWatch logo"
              className={`object-contain ${collapsed ? "h-7 w-7" : "h-9 w-9"}`}
            />
          </div>
          {!collapsed && (
            <div className="leading-tight">
              <div className="font-display text-sm font-semibold uppercase tracking-[0.18em] vw-text-glow">ProdWatch</div>
              <div className="font-mono-retro text-[10px] uppercase tracking-[0.3em] text-muted-foreground">Inventory · POS</div>
            </div>
          )}
        </div>
      </SidebarHeader>
      <SidebarContent>
        {mode === "inventory" && renderGroup("Inventory", inventoryItems)}
        {mode === "selling" && renderGroup("Selling / POS", sellingItems)}
        {renderGroup("Admin", adminItems)}
      </SidebarContent>
      {user && (
        <SidebarFooter className={collapsed ? "px-1 py-3" : "px-3 py-3"}>
          <div className={`flex items-center gap-2 ${collapsed ? "justify-center" : ""}`}>
            <Avatar className="h-8 w-8 shrink-0">
              <AvatarFallback className="bg-primary/20 text-xs text-primary-foreground">
                {user.name
                  .split(" ")
                  .map((p) => p[0])
                  .slice(0, 2)
                  .join("")}
              </AvatarFallback>
            </Avatar>
            {!collapsed && (
              <>
                <div className="min-w-0 flex-1 leading-tight">
                  <div className="truncate text-sm font-medium">{user.name}</div>
                  <div className="truncate text-xs text-muted-foreground">{user.email}</div>
                </div>
                <RoleBadge role={user.role} />
              </>
            )}
          </div>
        </SidebarFooter>
      )}
    </Sidebar>
  );
}
