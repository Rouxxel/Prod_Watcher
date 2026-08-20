import { useState } from "react";
import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { ArrowDown, KeyRound, Shield, Trash2, UserPlus, Users as UsersIcon } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { TableSkeleton } from "@/components/common/TableSkeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { RoleBadge } from "@/components/layout/RoleBadge";
import { ProvisionUserDialog } from "@/components/users/ProvisionUserDialog";
import { ResetPasswordDialog } from "@/components/users/ResetPasswordDialog";
import { StepDownAdminDialog } from "@/components/users/StepDownAdminDialog";
import {
  useDeactivateUser,
  useDeleteUser,
  usePromoteAdmin,
  useProvisionUser,
  useReactivateUser,
  useResetPassword,
  useStepDownAdmin,
  useUpdateUser,
  useUsers,
} from "@/hooks/queries";
import { useCurrentUser } from "@/hooks/use-current-user";
import { canAdminDeleteUser, canAdminResetPassword, canAdminStepDown } from "@/lib/role-access";
import { defaultPathForRole } from "@/lib/role-modes";
import { roleLabel } from "@/lib/format";
import { toast } from "sonner";
import type { Role, User } from "@/types";

export const Route = createFileRoute("/_app/users")({
  component: UsersPage,
});

const ASSIGNABLE_ROLES: Role[] = ["warehouse_manager", "warehouse_worker", "inspector", "cashier"];

function UsersPage() {
  const navigate = useNavigate();
  const { user: currentUser, refreshUser } = useCurrentUser();
  const users = useUsers();
  const provisionMut = useProvisionUser();
  const updateMut = useUpdateUser();
  const promoteMut = usePromoteAdmin();
  const stepDownMut = useStepDownAdmin();
  const resetMut = useResetPassword();
  const deactivateMut = useDeactivateUser();
  const deleteMut = useDeleteUser();
  const reactivateMut = useReactivateUser();

  const [provisionOpen, setProvisionOpen] = useState(false);
  const [resetUser, setResetUser] = useState<User | null>(null);
  const [promoteUser, setPromoteUser] = useState<User | null>(null);
  const [deleteUser, setDeleteUser] = useState<User | null>(null);
  const [stepDownOpen, setStepDownOpen] = useState(false);

  const userList = users.data ?? [];
  const showStepDown = canAdminStepDown(currentUser, userList);

  const toggleActive = (user: User, active: boolean) => {
    if (active) {
      reactivateMut.mutate(user.id, {
        onSuccess: () => toast.success(`${user.name} reactivated`),
      });
    } else {
      deactivateMut.mutate(user.id, {
        onSuccess: () => toast.success(`${user.name} deactivated`),
      });
    }
  };

  return (
    <div>
      <PageHeader
        title="Users & Roles"
        description="Manage team access and permissions."
        actions={
          <Button onClick={() => setProvisionOpen(true)}>
            <UserPlus className="mr-2 h-4 w-4" /> Provision user
          </Button>
        }
      />
      <Card className="p-4">
        {users.isLoading ? (
          <TableSkeleton rows={6} cols={5} />
        ) : users.isError ? (
          <EmptyState
            icon={UsersIcon}
            title="Unable to load users"
            description="Admin access is required."
          />
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
                  <TableHead className="w-[360px]">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.data?.map((u) => {
                  const initials = u.name
                    .split(" ")
                    .map((p) => p[0])
                    .slice(0, 2)
                    .join("");
                  const busy =
                    updateMut.isPending ||
                    promoteMut.isPending ||
                    stepDownMut.isPending ||
                    deleteMut.isPending ||
                    deactivateMut.isPending ||
                    reactivateMut.isPending;
                  const isSelf = currentUser?.id === u.id;
                  return (
                    <TableRow key={u.id}>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Avatar className="h-8 w-8">
                            <AvatarFallback className="bg-primary/20 text-xs">
                              {initials}
                            </AvatarFallback>
                          </Avatar>
                          <span className="font-medium">{u.name}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{u.email}</TableCell>
                      <TableCell>
                        {u.role === "admin" ? (
                          <RoleBadge role={u.role} />
                        ) : (
                          <Select
                            value={u.role}
                            disabled={busy}
                            onValueChange={(role) =>
                              updateMut.mutate(
                                { id: u.id, input: { role: role as Role } },
                                { onSuccess: () => toast.success(`Role updated for ${u.name}`) },
                              )
                            }
                          >
                            <SelectTrigger className="h-8 w-[180px]">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {ASSIGNABLE_ROLES.map((r) => (
                                <SelectItem key={r} value={r}>
                                  {roleLabel(r)}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      </TableCell>
                      <TableCell>
                        <Switch
                          checked={u.active}
                          disabled={busy}
                          onCheckedChange={(checked) => toggleActive(u, checked)}
                        />
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-wrap gap-2">
                          {u.role !== "admin" && (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={busy}
                              onClick={() => setPromoteUser(u)}
                            >
                              <Shield className="mr-1 h-3.5 w-3.5" />
                              Grant admin
                            </Button>
                          )}
                          {isSelf && showStepDown && (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={busy}
                              onClick={() => setStepDownOpen(true)}
                            >
                              <ArrowDown className="mr-1 h-3.5 w-3.5" />
                              Step down as admin
                            </Button>
                          )}
                          {currentUser && canAdminResetPassword(currentUser.id, u) && (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={busy}
                              onClick={() => setResetUser(u)}
                            >
                              <KeyRound className="mr-1 h-3.5 w-3.5" />
                              Reset password
                            </Button>
                          )}
                          {currentUser && canAdminDeleteUser(currentUser.id, u) && (
                            <Button
                              size="sm"
                              variant="outline"
                              disabled={busy}
                              className="text-destructive hover:text-destructive"
                              onClick={() => setDeleteUser(u)}
                            >
                              <Trash2 className="mr-1 h-3.5 w-3.5" />
                              Delete
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        )}
      </Card>

      <ProvisionUserDialog
        open={provisionOpen}
        onOpenChange={setProvisionOpen}
        pending={provisionMut.isPending}
        onSubmit={(input) =>
          provisionMut.mutate(input, {
            onSuccess: () => {
              toast.success(`User ${input.name} provisioned`);
              setProvisionOpen(false);
            },
          })
        }
      />

      <ResetPasswordDialog
        user={resetUser}
        open={!!resetUser}
        onOpenChange={(open) => !open && setResetUser(null)}
        pending={resetMut.isPending}
        onSubmit={(newPassword) => {
          if (!resetUser) return;
          resetMut.mutate(
            { id: resetUser.id, newPassword },
            {
              onSuccess: () => {
                toast.success(`Password updated for ${resetUser.name}`);
                setResetUser(null);
              },
            },
          );
        }}
      />

      <StepDownAdminDialog
        open={stepDownOpen}
        onOpenChange={setStepDownOpen}
        pending={stepDownMut.isPending}
        onSubmit={(role) =>
          stepDownMut.mutate(role, {
            onSuccess: async () => {
              toast.success(`You are now a ${roleLabel(role)}`);
              setStepDownOpen(false);
              if (typeof window !== "undefined") {
                window.localStorage.removeItem("prodwatch:app-mode");
              }
              await refreshUser();
              navigate({ to: defaultPathForRole(role) });
            },
          })
        }
      />

      <AlertDialog open={!!deleteUser} onOpenChange={(open) => !open && setDeleteUser(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete {deleteUser?.name}?</AlertDialogTitle>
            <AlertDialogDescription>
              This permanently removes their account and login access. Audit and stock movement
              history is kept, but users with POS sales cannot be deleted — deactivate them instead.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={() => {
                if (!deleteUser) return;
                deleteMut.mutate(deleteUser.id, {
                  onSuccess: () => {
                    toast.success(`${deleteUser.name} deleted`);
                    setDeleteUser(null);
                  },
                });
              }}
            >
              Delete user
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={!!promoteUser} onOpenChange={(open) => !open && setPromoteUser(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Grant admin access?</AlertDialogTitle>
            <AlertDialogDescription>
              {promoteUser?.name} will receive full admin permissions. This cannot be undone from
              the role dropdown — only another admin can change their role later.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                if (!promoteUser) return;
                promoteMut.mutate(promoteUser.id, {
                  onSuccess: () => {
                    toast.success(`${promoteUser.name} is now an admin`);
                    setPromoteUser(null);
                  },
                });
              }}
            >
              Confirm
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
