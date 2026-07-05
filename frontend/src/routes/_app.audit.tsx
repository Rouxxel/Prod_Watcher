import { createFileRoute } from "@tanstack/react-router";
import { ClipboardCheck } from "lucide-react";
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
import { useAudit, useUsers } from "@/hooks/queries";
import { dateTime } from "@/lib/format";

export const Route = createFileRoute("/_app/audit")({
  component: AuditPage,
});

function AuditPage() {
  const audit = useAudit();
  const users = useUsers();
  const userName = (id: string) => users.data?.find((u) => u.id === id)?.name ?? "—";

  return (
    <div>
      <PageHeader title="Inventory Audit" description="Immutable log of inventory-related user actions." />
      <Card className="p-4">
        {audit.isLoading ? (
          <TableSkeleton rows={6} cols={5} />
        ) : (audit.data?.length ?? 0) === 0 ? (
          <EmptyState icon={ClipboardCheck} title="No audit entries" />
        ) : (
          <div className="overflow-hidden rounded-md border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Action</TableHead>
                  <TableHead>Entity</TableHead>
                  <TableHead>User</TableHead>
                  <TableHead>Details</TableHead>
                  <TableHead>When</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {audit.data?.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell><Badge variant="outline" className="bg-primary/15 text-primary-foreground border-primary/30">{a.action}</Badge></TableCell>
                    <TableCell className="text-muted-foreground">{a.entity} · {a.entityId}</TableCell>
                    <TableCell>{userName(a.userId)}</TableCell>
                    <TableCell className="text-muted-foreground">{a.details ?? "—"}</TableCell>
                    <TableCell className="text-muted-foreground">{dateTime(a.timestamp)}</TableCell>
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
