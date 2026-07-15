import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { auditService } from "@/services/audit.service";
import { movementsService } from "@/services/movements.service";
import { productsService } from "@/services/products.service";
import { transactionsService } from "@/services/transactions.service";
import { usersService } from "@/services/users.service";
import { warehousesService } from "@/services/warehouses.service";
import type {
  ProductInput,
  Role,
  StockMovementInput,
  UserProvisionInput,
  UserUpdateInput,
  WarehouseInput,
} from "@/types";

export const useProducts = (warehouseId?: string) =>
  useQuery({
    queryKey: ["products", warehouseId ?? "all"],
    queryFn: () => productsService.list(warehouseId),
  });

export const useWarehouses = () =>
  useQuery({ queryKey: ["warehouses"], queryFn: () => warehousesService.list() });

export const useMovements = () =>
  useQuery({ queryKey: ["movements"], queryFn: () => movementsService.list() });

export const useAudit = () =>
  useQuery({ queryKey: ["audit"], queryFn: () => auditService.list() });

export const useTransactions = () =>
  useQuery({ queryKey: ["transactions"], queryFn: () => transactionsService.list() });

export const useUsers = () =>
  useQuery({ queryKey: ["users"], queryFn: () => usersService.list() });

export const useCreateProduct = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: ProductInput) => productsService.create(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
  });
};

export const useUpdateProduct = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: ProductInput }) =>
      productsService.update(id, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
  });
};

export const useDeleteProduct = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => productsService.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
  });
};

export const useCreateWarehouse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: WarehouseInput) => warehousesService.create(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["warehouses"] }),
  });
};

export const useUpdateWarehouse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: WarehouseInput }) =>
      warehousesService.update(id, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["warehouses"] }),
  });
};

export const useDeleteWarehouse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => warehousesService.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["warehouses"] });
      qc.invalidateQueries({ queryKey: ["products"] });
    },
  });
};

export const useCreateMovement = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: StockMovementInput) => movementsService.create(input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["products"] });
      qc.invalidateQueries({ queryKey: ["movements"] });
      qc.invalidateQueries({ queryKey: ["audit"] });
    },
  });
};

export const useProvisionUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: UserProvisionInput) => usersService.provision(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
};

export const useUpdateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: UserUpdateInput }) =>
      usersService.update(id, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
};

export const usePromoteAdmin = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersService.promoteAdmin(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
};

export const useStepDownAdmin = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (role: Role) => usersService.stepDownAdmin(role),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
};

export const useResetPassword = () => {
  return useMutation({
    mutationFn: ({ id, newPassword }: { id: string; newPassword: string }) =>
      usersService.resetPassword(id, newPassword),
  });
};

export const useDeactivateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersService.deactivate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
};

export const useReactivateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersService.reactivate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
};

export const useDeleteUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersService.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
};
