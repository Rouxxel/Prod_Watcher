import { users as seed } from "@/mock/seed";
import type { User } from "@/types";
import { fakeDelay } from "./mock-utils";

export const usersService = {
  async list(): Promise<User[]> {
    await fakeDelay();
    return [...seed];
  },
};
