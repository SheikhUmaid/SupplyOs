import { create } from "zustand";
import type { CurrentUser } from "@/types/api";

export type AuthStatus = "loading" | "authenticated" | "anonymous";

interface AuthState {
  status: AuthStatus;
  user: CurrentUser | null;
  setUser: (user: CurrentUser | null) => void;
}

/** Display data only, and never persisted. The JWT lives in an HttpOnly cookie out of JS's reach. */
export const useAuthStore = create<AuthState>()((set) => ({
  status: "loading",
  user: null,
  setUser: (user) => set({ user, status: user ? "authenticated" : "anonymous" }),
}));
