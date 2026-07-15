import { Outlet, createFileRoute, useRouterState, Navigate, redirect } from "@tanstack/react-router";
import { useEffect } from "react";
import { SidebarProvider, SidebarInset } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/layout/AppSidebar";
import { Topbar } from "@/components/layout/Topbar";
import { ModeSelectScreen } from "@/components/layout/ModeSelectScreen";
import { CRTOverlay } from "@/components/layout/CRTOverlay";
import { useAppMode } from "@/hooks/use-app-mode";
import { useCurrentUser } from "@/hooks/use-current-user";
import { getAccessToken } from "@/lib/auth-token";
import { allowedModesForRole, canUseAppMode, defaultAppModeForRole } from "@/lib/role-modes";

export const Route = createFileRoute("/_app")({
  beforeLoad: () => {
    if (typeof window !== "undefined" && !getAccessToken()) {
      throw redirect({ to: "/login" });
    }
  },
  component: AppLayout,
});

const INVENTORY_PATHS = ["/", "/products", "/warehouses", "/stock-movements", "/audit"];
const SELLING_PATHS = ["/cashier", "/cart", "/transactions"];
const ADMIN_PATHS = ["/users", "/settings"];

function AppLayout() {
  const { user, isLoading } = useCurrentUser();
  const { mode, setMode } = useAppMode();
  const path = useRouterState({ select: (s) => s.location.pathname });

  useEffect(() => {
    if (!user) return;
    const allowed = allowedModesForRole(user.role);
    if (allowed.length === 1) {
      if (mode !== allowed[0]) setMode(allowed[0]);
      return;
    }
    if (mode && !canUseAppMode(user.role, mode)) {
      setMode(defaultAppModeForRole(user.role));
    }
  }, [user, mode, setMode]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <p className="text-sm text-muted-foreground">Loading…</p>
      </div>
    );
  }

  if (!user) return <Navigate to="/login" />;
  if (!mode) {
    const allowed = allowedModesForRole(user.role);
    if (allowed.length === 1) {
      return (
        <div className="flex min-h-screen items-center justify-center bg-background">
          <p className="text-sm text-muted-foreground">Loading…</p>
        </div>
      );
    }
    return <ModeSelectScreen />;
  }

  const isAdmin = ADMIN_PATHS.some((p) => path === p || path.startsWith(p + "/"));
  const inInventory = INVENTORY_PATHS.some((p) => (p === "/" ? path === "/" : path.startsWith(p)));
  const inSelling = SELLING_PATHS.some((p) => path.startsWith(p));

  if (!canUseAppMode(user.role, "selling") && (mode === "selling" || inSelling)) {
    return <Navigate to="/" />;
  }

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
