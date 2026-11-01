"use client";

import { MotionConfig } from "framer-motion";
import { useEffect, type ReactNode } from "react";
import { Toaster } from "@/components/ui/Toaster";
import { setUnauthorizedHandler } from "@/services/api/client";
import { authService } from "@/services/auth/auth";
import { useAuthStore } from "@/stores/auth/authStore";
import { useCartStore } from "@/stores/cart/cartStore";
import { useUiStore } from "@/stores/ui/uiStore";

export function Providers({ children }: { children: ReactNode }) {
  // Persisted stores load after mount so the first client render matches the server's HTML.
  useEffect(() => {
    void useCartStore.persist.rehydrate();
    void useUiStore.persist.rehydrate();
  }, []);

  useEffect(() => {
    const { setUser } = useAuthStore.getState();
    setUnauthorizedHandler(() => setUser(null));
    authService
      .me()
      .then(setUser)
      .catch(() => setUser(null));
    return () => setUnauthorizedHandler(null);
  }, []);

  return (
    <MotionConfig reducedMotion="user">
      {children}
      <Toaster />
    </MotionConfig>
  );
}
