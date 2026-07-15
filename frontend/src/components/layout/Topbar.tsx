import { SidebarTrigger } from "@/components/ui/sidebar";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { Boxes, ScanBarcode, LogOut } from "lucide-react";
import { useCurrentUser } from "@/hooks/use-current-user";
import { useAppMode, type AppMode } from "@/hooks/use-app-mode";
import { useNavigate } from "@tanstack/react-router";
import { allowedModesForRole, canSwitchAppMode } from "@/lib/role-modes";

export function Topbar() {
  const { user, logout } = useCurrentUser();
  const { mode, setMode } = useAppMode();
  const navigate = useNavigate();

  if (!user) return null;

  const modes = allowedModesForRole(user.role);
  const modeLabel = mode === "selling" ? "Selling" : mode === "inventory" ? "Inventory" : "—";

  const handleLogout = () => {
    void logout().then(() => navigate({ to: "/login" }));
  };

  return (
    <header className="sticky top-0 z-30 flex h-14 items-center justify-between gap-3 border-b border-border bg-background/80 px-3 backdrop-blur sm:px-5">
      <div className="flex items-center gap-2">
        <SidebarTrigger />
        <div className="hidden text-sm text-muted-foreground sm:block">
          <span className="text-foreground/80">ProdWatch</span>
          <span className="mx-2 opacity-40">/</span>
          <span>{modeLabel}</span>
        </div>
      </div>
      <div className="flex items-center gap-3">
        {canSwitchAppMode(user.role) && (
          <Select
            value={mode ?? undefined}
            onValueChange={(v) => setMode(v as AppMode)}
          >
            <SelectTrigger className="h-8 w-[160px] gap-1.5" aria-label="Switch mode">
              <SelectValue placeholder="Select mode" />
            </SelectTrigger>
            <SelectContent>
              {modes.includes("inventory") && (
                <SelectItem value="inventory">
                  <span className="flex items-center gap-2">
                    <Boxes className="h-3.5 w-3.5" />
                    Inventory mode
                  </span>
                </SelectItem>
              )}
              {modes.includes("selling") && (
                <SelectItem value="selling">
                  <span className="flex items-center gap-2">
                    <ScanBarcode className="h-3.5 w-3.5" />
                    Selling mode
                  </span>
                </SelectItem>
              )}
            </SelectContent>
          </Select>
        )}

        <Button
          variant="outline"
          size="sm"
          onClick={handleLogout}
          className="h-8 gap-1.5"
          title="Log out"
        >
          <LogOut className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Log out</span>
        </Button>
      </div>
    </header>
  );
}
