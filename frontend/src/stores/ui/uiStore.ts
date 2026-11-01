import { create } from "zustand";
import { persist } from "zustand/middleware";

export type ToastTone = "success" | "error" | "info";

export interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

interface UiState {
  sidebarCollapsed: boolean;
  mobileNavOpen: boolean;
  toasts: Toast[];
  toggleSidebar: () => void;
  setMobileNavOpen: (open: boolean) => void;
  pushToast: (tone: ToastTone, message: string) => void;
  dismissToast: (id: number) => void;
  clearToasts: () => void;
}

let nextToastId = 1;

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      sidebarCollapsed: false,
      mobileNavOpen: false,
      toasts: [],
      toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
      setMobileNavOpen: (mobileNavOpen) => set({ mobileNavOpen }),
      pushToast: (tone, message) =>
        set((state) => ({ toasts: [...state.toasts, { id: nextToastId++, tone, message }] })),
      clearToasts: () => set({ toasts: [] }),
      dismissToast: (id) =>
        set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) })),
    }),
    {
      name: "scos-ui",
      skipHydration: true,
      partialize: (state) => ({ sidebarCollapsed: state.sidebarCollapsed }),
    },
  ),
);
