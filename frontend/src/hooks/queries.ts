import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toastApiError } from "@/lib/api-error";
import { auditService } from "@/services/audit.service";
import { movementsService } from "@/services/movements.service";
import { productsService } from "@/services/products.service";
import { transactionsService } from "@/services/transactions.service";
import { usersService } from "@/services/users.service";
import { warehousesService } from "@/services/warehouses.service";
import type {
  ProductInput,
  StockMovementInput,
  UserProvisionInput,
  UserUpdateInput,
  WarehouseInput,
} from "@/types";

const onMutationError = (err: unknown) => toastApiError(err);

export const useProducts = () =>
  useQuery({ queryKey: ["products"], queryFn: () => productsService.list() });

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
    onError: onMutationError,
  });
};

export const useUpdateProduct = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: ProductInput }) =>
      productsService.update(id, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
    onError: onMutationError,
  });
};

export const useDeleteProduct = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => productsService.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
    onError: onMutationError,
  });
};

export const useCreateWarehouse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: WarehouseInput) => warehousesService.create(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["warehouses"] }),
    onError: onMutationError,
  });
};

export const useUpdateWarehouse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: WarehouseInput }) =>
      warehousesService.update(id, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["warehouses"] }),
    onError: onMutationError,
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
    onError: onMutationError,
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
    onError: onMutationError,
  });
};

export const useProvisionUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: UserProvisionInput) => usersService.provision(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
    onError: onMutationError,
  });
};

export const useUpdateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: UserUpdateInput }) =>
      usersService.update(id, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
    onError: onMutationError,
  });
};

export const usePromoteAdmin = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersService.promoteAdmin(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
    onError: onMutationError,
  });
};

export const useResetPassword = () => {
  return useMutation({
    mutationFn: ({ id, newPassword }: { id: string; newPassword: string }) =>
      usersService.resetPassword(id, newPassword),
    onError: onMutationError,
  });
};

export const useDeactivateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersService.deactivate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
    onError: onMutationError,
  });
};

export const useReactivateUser = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => usersService.reactivate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
    onError: onMutationError,
  });
};
