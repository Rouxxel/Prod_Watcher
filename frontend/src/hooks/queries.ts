import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { productsService } from "@/services/products.service";
import { warehousesService } from "@/services/warehouses.service";
import { movementsService } from "@/services/movements.service";
import { auditService } from "@/services/audit.service";
import { transactionsService } from "@/services/transactions.service";
import { usersService } from "@/services/users.service";
import type { ProductInput } from "@/types";

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
