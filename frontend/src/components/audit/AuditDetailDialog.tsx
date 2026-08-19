import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { auditUserLabel, dateTime, shortId } from "@/lib/format";
import type { AuditEntry } from "@/types";

interface Props {
  entry: AuditEntry | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

function DetailField({
  label,
  children,
  className,
}: {
  label: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={className}>
      <dt className="text-xs font-medium text-muted-foreground">{label}</dt>
      <dd className="mt-1 text-sm">{children}</dd>
    </div>
  );
}

export function AuditDetailDialog({ entry, open, onOpenChange }: Props) {
  if (!entry) return null;

  const entityDisplay = `${entry.entity} · ${entry.entityLabel ?? shortId(entry.entityId)}`;
  const userName = auditUserLabel(entry.userName, entry.userId);
  const details = entry.details?.trim();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Audit entry</DialogTitle>
        </DialogHeader>
        <dl className="relative z-[1] grid grid-cols-2 gap-x-4 gap-y-4">
          <DetailField label="Action">
            <Badge
              variant="outline"
              className="bg-primary/15 text-primary-foreground border-primary/30"
            >
              {entry.action}
            </Badge>
          </DetailField>
          <DetailField label="Entity">{entityDisplay}</DetailField>
          <DetailField label="User">{userName}</DetailField>
          <DetailField label="When">{dateTime(entry.timestamp)}</DetailField>
          {details && (
            <DetailField label="Details" className="col-span-2">
              <p className="whitespace-pre-wrap text-muted-foreground">{details}</p>
            </DetailField>
          )}
        </dl>
        <DialogFooter>
          <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
