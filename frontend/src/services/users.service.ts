import type { Role, User, UserProvisionInput, UserUpdateInput } from "@/types";
import { apiGet, apiPatch, apiPost, apiDelete } from "./api";
import { mapUser, type ApiUserResponse } from "./auth.service";

function mapUsers(rows: ApiUserResponse[]): User[] {
  return rows.map(mapUser);
}

export const usersService = {
  async list(): Promise<User[]> {
    const rows = await apiGet<ApiUserResponse[]>("/users");
    return mapUsers(rows);
  },

  async provision(input: UserProvisionInput): Promise<User> {
    const row = await apiPost<ApiUserResponse>("/users", input);
    return mapUser(row);
  },

  async update(id: string, input: UserUpdateInput): Promise<User> {
    const row = await apiPatch<ApiUserResponse>(`/users/${id}`, input);
    return mapUser(row);
  },

  async promoteAdmin(id: string): Promise<User> {
    const row = await apiPost<ApiUserResponse>(`/users/${id}/promote-admin`);
    return mapUser(row);
  },

  async stepDownAdmin(role: Role): Promise<User> {
    const row = await apiPost<ApiUserResponse>("/users/me/step-down-admin", { role });
    return mapUser(row);
  },

  async resetPassword(id: string, newPassword: string): Promise<void> {
    await apiPost(`/users/${id}/reset-password`, { newPassword });
  },

  async deactivate(id: string): Promise<User> {
    const row = await apiPost<ApiUserResponse>(`/users/${id}/deactivate`);
    return mapUser(row);
  },

  async reactivate(id: string): Promise<User> {
    const row = await apiPost<ApiUserResponse>(`/users/${id}/reactivate`);
    return mapUser(row);
  },

  async delete(id: string): Promise<void> {
    await apiDelete(`/users/${id}`);
  },
};
