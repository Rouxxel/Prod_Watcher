import { useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { Users as UsersIcon, UserPlus } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
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
import { RoleBadge } from "@/components/layout/RoleBadge";
import { InviteUserDialog } from "@/components/users/InviteUserDialog";
import { useUsers } from "@/hooks/queries";
import { toast } from "sonner";

export const Route = createFileRoute("/_app/users")({
  component: UsersPage,
});

function UsersPage() {
  const users = useUsers();
  const [inviteOpen, setInviteOpen] = useState(false);

  return (
    <div>
      <PageHeader
        title="Users & Roles"
        description="Manage team access and permissions."
        actions={
          <Button onClick={() => setInviteOpen(true)}>
            <UserPlus className="mr-2 h-4 w-4" /> Invite user
          </Button>
        }
      />
      <Card className="p-4">
        {users.isLoading ? (
          <TableSkeleton rows={6} cols={4} />
        ) : (users.data?.length ?? 0) === 0 ? (
          <EmptyState icon={UsersIcon} title="No users" />
        ) : (
          <div className="overflow-hidden rounded-md border border-border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>User</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead className="w-[100px]">Active</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.data?.map((u) => {
                  const initials = u.name.split(" ").map((p) => p[0]).slice(0, 2).join("");
                  return (
                    <TableRow key={u.id}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Avatar className="h-8 w-8">
                            <AvatarFallback className="bg-primary/20 text-xs">{initials}</AvatarFallback>
                          </Avatar>
                          <span className="font-medium">{u.name}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{u.email}</TableCell>
                      <TableCell><RoleBadge role={u.role} /></TableCell>
                      <TableCell>
                        <Switch defaultChecked={u.active} onCheckedChange={() => toast.message("Status toggle is UI-only")} />
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        )}
      </Card>

      <InviteUserDialog
        open={inviteOpen}
        onOpenChange={setInviteOpen}
        onSubmit={(input) => {
          toast.success(
            `Invite sent to ${input.email} as ${input.role}${input.active ? "" : " (inactive)"}`,
          );
          setInviteOpen(false);
        }}
      />
    </div>
  );
}

