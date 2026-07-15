import type { Role, User } from "@/types";
import { apiGet, apiPost } from "./api";

export interface BootstrapStatus {
  signupAllowed: boolean;
}

export interface LoginInput {
  email: string;
  password: string;
}

export interface SignupInput {
  name: string;
  email: string;
  password: string;
}

interface ApiUserResponse {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
  emailConfirmed?: boolean | null;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string | null;
  expiresIn?: number | null;
  user: ApiUserResponse;
}

export interface SignupResponse {
  message: string;
}

export interface SignupEmailAvailability {
  available: boolean;
}

export function mapUser(response: ApiUserResponse): User {
  return {
    id: response.id,
    name: response.name,
    email: response.email,
    role: response.role,
    active: response.active,
  };
}

export const authService = {
  bootstrapStatus(): Promise<BootstrapStatus> {
    return apiGet<BootstrapStatus>("/auth/bootstrap-status", { auth: false });
  },

  signupEmailAvailable(email: string): Promise<SignupEmailAvailability> {
    const params = new URLSearchParams({ email: email.trim().toLowerCase() });
    return apiGet<SignupEmailAvailability>(`/auth/signup-email-available?${params}`, { auth: false });
  },

  signup(input: SignupInput): Promise<SignupResponse> {
    return apiPost<SignupResponse>("/auth/signup", input, { auth: false });
  },

  login(input: LoginInput): Promise<LoginResponse> {
    return apiPost<LoginResponse>("/auth/login", input, { auth: false });
  },

  logout(): Promise<void> {
    return apiPost<void>("/auth/logout");
  },

  confirmEmail(token: string): Promise<LoginResponse> {
    return apiPost<LoginResponse>("/auth/confirm-email", { token }, { auth: false });
  },

  getMe(): Promise<User> {
    return apiGet<ApiUserResponse>("/users/me").then(mapUser);
  },
};
