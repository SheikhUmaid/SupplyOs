import { useRouter } from "next/navigation";
import { authService } from "@/services/auth/auth";
import { useAuthStore } from "@/stores/auth/authStore";
import { useUiStore } from "@/stores/ui/uiStore";

export function useLogout(redirectTo: string): () => Promise<void> {
  const router = useRouter();
  const setUser = useAuthStore((state) => state.setUser);
  const pushToast = useUiStore((state) => state.pushToast);

  return async () => {
    try {
      await authService.logout();
      setUser(null);
      router.push(redirectTo);
    } catch {
      // The session cookie can only be cleared by the server, so don't pretend it has been.
      pushToast("error", "Could not log out. Please try again.");
    }
  };
}
