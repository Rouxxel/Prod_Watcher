import type { Role } from "@/types";
import { Badge } from "@/components/ui/badge";
import { roleLabel } from "@/lib/format";
import { cn } from "@/lib/utils";

const roleStyles: Record<Role, string> = {
  admin: "bg-primary/20 text-primary-foreground border-primary/40",
  warehouse_manager: "bg-info/15 text-info border-info/30",
  warehouse_worker: "bg-muted text-muted-foreground border-border",
  inspector: "bg-warning/15 text-warning border-warning/30",
  cashier: "bg-success/15 text-success border-success/30",
};

export function RoleBadge({ role, className }: { role: Role; className?: string }) {
  return (
    <Badge variant="outline" className={cn("font-medium", roleStyles[role], className)}>
      {roleLabel(role)}
    </Badge>
  );
}
