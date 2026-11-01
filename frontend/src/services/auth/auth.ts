import { api } from "@/services/api/client";
import type { CurrentUser, RegisterRequest } from "@/types/api";

export interface LoginRequest {
  email: string;
  password: string;
}

export const authService = {
  register: (body: RegisterRequest) => api.post<CurrentUser>("/auth/register", { body }),
  login: (body: LoginRequest) => api.post<CurrentUser>("/auth/login", { body }),
  logout: () => api.post<void>("/auth/logout"),
  me: () => api.get<CurrentUser>("/auth/me"),
};
