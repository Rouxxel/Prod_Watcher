import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { ArrowLeftRight, Plus } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { TableSkeleton } from "@/components/common/TableSkeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { MovementFormDialog } from "@/components/movements/MovementFormDialog";
import { MovementDetailDialog } from "@/components/movements/MovementDetailDialog";
import { useCreateMovement, useMovements, useProducts, useWarehouses } from "@/hooks/queries";
import { useCurrentUser } from "@/hooks/use-current-user";
import { canMutateInventory } from "@/lib/role-access";
import { dateTime } from "@/lib/format";
import { toast } from "sonner";
import type { StockMovement } from "@/types";

export const Route = createFileRoute("/_app/stock-movements")({
  component: MovementsPage,
});

const typeColor: Record<string, string> = {
  IN: "bg-success/15 text-success border-success/30",
  OUT: "bg-destructive/15 text-destructive border-destructive/30",
  TRANSFER: "bg-info/15 text-info border-info/30",
  ADJUSTMENT: "bg-warning/15 text-warning border-warning/30",
};

function MovementsPage() {
  const { user } = useCurrentUser();
  const readOnly = user ? !canMutateInventory(user.role) : false;
  const movements = useMovements();
  const products = useProducts();
  const warehouses = useWarehouses();
  const createMut = useCreateMovement();
  const [filter, setFilter] = useState<string>("all");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selected, setSelected] = useState<StockMovement | null>(null);

  const data = (movements.data ?? []).filter((m) => filter === "all" || m.type === filter);

  const productName = (m: StockMovement) =>
    m.productName ?? products.data?.find((p) => p.id === m.productId)?.name ?? m.productId;
  const userName = (m: StockMovement) => m.userName ?? "—";
  const fromLabel = (m: StockMovement) =>
    m.type === "IN" ? m.provider ?? "—" : whName(m.fromWarehouseId);
  const toLabel = (m: StockMovement) =>
    m.type === "OUT" ? m.recipient ?? "—" : whName(m.toWarehouseId);

  const whName = (id?: string) => (id ? warehouses.data?.find((w) => w.id === id)?.name ?? "—" : "—");

  return (
    <div>
      <PageHeader
        title="Stock Movements"
        description={
          readOnly
            ? "Read-only history of IN, OUT, transfer, and adjustment activity."
            : "Every IN, OUT, transfer, and adjustment across the network."
        }
        actions={
          !readOnly ? (
            <Button onClick={() => setDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" /> Record movement
            </Button>
          ) : undefined
        }
      />
      <Card className="p-4">
        <div className="mb-4 flex items-center justify-between">
          <Select value={filter} onValueChange={setFilter}>
            <SelectTrigger className="w-[180px]"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All types</SelectItem>
              <SelectItem value="IN">IN</SelectItem>
              <SelectItem value="OUT">OUT</SelectItem>
              <SelectItem value="TRANSFER">TRANSFER</SelectItem>
              <SelectItem value="ADJUSTMENT">ADJUSTMENT</SelectItem>
            </SelectContent>
          </Select>
          <div className="text-xs text-muted-foreground">{data.length} entries</div>
        </div>

        {movements.isLoading ? (
          <TableSkeleton rows={6} cols={6} />
        ) : data.length === 0 ? (
          <EmptyState
            icon={ArrowLeftRight}
            title="No movements"
            description="No records match this filter."
            action={
              !readOnly ? (
                <Button onClick={() => setDialogOpen(true)}>
                  <Plus className="mr-2 h-4 w-4" /> Record movement
                </Button>
              ) : undefined
            }
          />
        ) : (
          <div className="overflow-hidden rounded-md border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Product</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead className="text-right">Qty</TableHead>
                  <TableHead>From</TableHead>
                  <TableHead>To</TableHead>
                  <TableHead>User</TableHead>
                  <TableHead>When</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((m) => (
                  <TableRow
                    key={m.id}
                    className="cursor-pointer hover:bg-muted/50"
                    onClick={() => setSelected(m)}
                  >
                    <TableCell className="font-medium">{productName(m)}</TableCell>
                    <TableCell><Badge variant="outline" className={typeColor[m.type]}>{m.type}</Badge></TableCell>
                    <TableCell className="text-right tabular-nums">{m.qty > 0 ? `+${m.qty}` : m.qty}</TableCell>
                    <TableCell className="text-muted-foreground">{fromLabel(m)}</TableCell>
                    <TableCell className="text-muted-foreground">{toLabel(m)}</TableCell>
                    <TableCell>{userName(m)}</TableCell>
                    <TableCell className="text-muted-foreground">{dateTime(m.timestamp)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Card>

      {!readOnly && (
        <MovementFormDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          products={products.data ?? []}
          warehouses={warehouses.data ?? []}
          pending={createMut.isPending}
          onSubmit={(input) =>
            createMut.mutate(input, {
              onSuccess: () => {
                toast.success("Movement recorded");
                setDialogOpen(false);
              },
            })
          }
        />
      )}

      <MovementDetailDialog
        movement={selected}
        open={selected !== null}
        onOpenChange={(open) => !open && setSelected(null)}
        products={products.data ?? []}
        warehouses={warehouses.data ?? []}
      />
    </div>
  );
}
