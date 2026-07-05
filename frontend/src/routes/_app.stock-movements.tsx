import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { ArrowLeftRight } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
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
import { useMovements, useProducts, useUsers, useWarehouses } from "@/hooks/queries";
import { dateTime } from "@/lib/format";

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
  const movements = useMovements();
  const products = useProducts();
  const users = useUsers();
  const warehouses = useWarehouses();
  const [filter, setFilter] = useState<string>("all");

  const data = (movements.data ?? []).filter((m) => filter === "all" || m.type === filter);

  const productName = (id: string) => products.data?.find((p) => p.id === id)?.name ?? id;
  const userName = (id: string) => users.data?.find((u) => u.id === id)?.name ?? "—";
  const whName = (id?: string) => (id ? warehouses.data?.find((w) => w.id === id)?.name ?? "—" : "—");

  return (
    <div>
      <PageHeader title="Stock Movements" description="Every IN, OUT, transfer, and adjustment across the network." />
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
          <EmptyState icon={ArrowLeftRight} title="No movements" description="No records match this filter." />
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
                  <TableRow key={m.id}>
                    <TableCell className="font-medium">{productName(m.productId)}</TableCell>
                    <TableCell><Badge variant="outline" className={typeColor[m.type]}>{m.type}</Badge></TableCell>
                    <TableCell className="text-right tabular-nums">{m.qty > 0 ? `+${m.qty}` : m.qty}</TableCell>
                    <TableCell className="text-muted-foreground">{whName(m.fromWarehouseId)}</TableCell>
                    <TableCell className="text-muted-foreground">{whName(m.toWarehouseId)}</TableCell>
                    <TableCell>{userName(m.userId)}</TableCell>
                    <TableCell className="text-muted-foreground">{dateTime(m.timestamp)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Card>
    </div>
  );
}
