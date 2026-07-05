import { Outlet, createFileRoute, useRouterState, Navigate } from "@tanstack/react-router";
import { SidebarProvider, SidebarInset } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/layout/AppSidebar";
import { Topbar } from "@/components/layout/Topbar";
import { ModeSelectScreen } from "@/components/layout/ModeSelectScreen";
import { CRTOverlay } from "@/components/layout/CRTOverlay";
import { useAppMode } from "@/hooks/use-app-mode";
import { useCurrentUser } from "@/hooks/use-current-user";

export const Route = createFileRoute("/_app")({
  component: AppLayout,
});

const INVENTORY_PATHS = ["/", "/products", "/warehouses", "/stock-movements", "/audit"];
const SELLING_PATHS = ["/cashier", "/cart", "/transactions"];
const ADMIN_PATHS = ["/users", "/settings"];

function AppLayout() {
  const { user } = useCurrentUser();
  const { mode } = useAppMode();
  const path = useRouterState({ select: (s) => s.location.pathname });

  if (!user) return <Navigate to="/login" />;
  if (!mode) return <ModeSelectScreen />;

  const isAdmin = ADMIN_PATHS.some((p) => path === p || path.startsWith(p + "/"));
  const inInventory = INVENTORY_PATHS.some((p) => (p === "/" ? path === "/" : path.startsWith(p)));
  const inSelling = SELLING_PATHS.some((p) => path.startsWith(p));

  if (!isAdmin) {
    if (mode === "inventory" && inSelling) return <Navigate to="/" />;
    if (mode === "selling" && inInventory) return <Navigate to="/cashier" />;
  }

  return (
    <SidebarProvider>
      <div aria-hidden className="vw-ambient-grid" />
      <div
        aria-hidden
        className="vw-ambient-glow"
        style={{ top: "-10%", left: "-10%", width: "60vw", height: "60vh", background: "var(--gradient-sun)" }}
      />
      <div
        aria-hidden
        className="vw-ambient-glow"
        style={{ bottom: "-20%", right: "-10%", width: "55vw", height: "55vh", background: "var(--gradient-accent)", opacity: 0.4 }}
      />
      <div className="relative z-10 flex min-h-screen w-full">
        <AppSidebar />
        <SidebarInset className="flex flex-1 flex-col bg-transparent">
          <Topbar />
          <main className="flex-1 p-4 sm:p-6 lg:p-8">
            <Outlet />
          </main>
        </SidebarInset>
      </div>
      <CRTOverlay />
    </SidebarProvider>
  );
}
