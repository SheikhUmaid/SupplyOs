"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { Skeleton } from "@/components/ui/Feedback";
import { homeFor } from "@/lib/authRoutes";
import { useAuthStore } from "@/stores/auth/authStore";
import type { UserRole } from "@/types/api";

/** Sends away anyone without the role. It only shapes the UI; the API enforces this regardless. */
function RoleGuard({ role, children }: { role: UserRole; children: ReactNode }) {
  const status = useAuthStore((state) => state.status);
  const actualRole = useAuthStore((state) => state.user?.role);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "anonymous") router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    if (actualRole && actualRole !== role) router.replace(homeFor(actualRole));
  }, [status, actualRole, role, pathname, router]);

  if (actualRole === role) return children;

  return (
    <div role="status" aria-label="Checking your access" className="space-y-3 p-6">
      <Skeleton className="h-6 w-48" />
      <Skeleton className="h-32 w-full" />
    </div>
  );
}

export function AdminGuard({ children }: { children: ReactNode }) {
  return <RoleGuard role="ADMIN">{children}</RoleGuard>;
}

export function CustomerGuard({ children }: { children: ReactNode }) {
  return <RoleGuard role="CUSTOMER">{children}</RoleGuard>;
}
