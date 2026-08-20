import { createFileRoute } from "@tanstack/react-router";
import { Receipt } from "lucide-react";
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
import { TableSkeleton } from "@/components/common/TableSkeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { useTransactions } from "@/hooks/queries";
import { currency, dateTime } from "@/lib/format";
import type { TransactionStatus } from "@/types";

export const Route = createFileRoute("/_app/transactions")({
  component: TransactionsPage,
});

const statusStyle: Record<TransactionStatus, string> = {
  completed: "bg-success/15 text-success border-success/30",
  refunded: "bg-warning/15 text-warning border-warning/30",
  void: "bg-destructive/15 text-destructive border-destructive/30",
};

function shortId(id: string) {
  return id.length > 8 ? `${id.slice(0, 8).toUpperCase()}…` : id.toUpperCase();
}

function TransactionsPage() {
  const tx = useTransactions();

  return (
    <div>
      <PageHeader title="Transactions" description="History of point-of-sale activity." />
      <Card className="p-4">
        {tx.isLoading ? (
          <TableSkeleton rows={6} cols={5} />
        ) : tx.isError ? (
          <EmptyState
            icon={Receipt}
            title="Unable to load transactions"
            description="You may not have permission to view sales history."
          />
        ) : (tx.data?.length ?? 0) === 0 ? (
          <EmptyState icon={Receipt} title="No transactions yet" />
        ) : (
          <div className="overflow-hidden rounded-md border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>Cashier</TableHead>
                  <TableHead className="text-right">Items</TableHead>
                  <TableHead className="text-right">Total</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>When</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tx.data?.map((t) => (
                  <TableRow key={t.id}>
                    <TableCell className="font-medium">#{shortId(t.id)}</TableCell>
                    <TableCell className="text-muted-foreground" title={t.cashierId}>
                      {t.cashierName ?? shortId(t.cashierId)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {t.items.reduce((s, i) => s + i.qty, 0)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{currency(t.total)}</TableCell>
                    <TableCell>
                      <Badge variant="outline" className={statusStyle[t.status]}>
                        {t.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{dateTime(t.timestamp)}</TableCell>
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
