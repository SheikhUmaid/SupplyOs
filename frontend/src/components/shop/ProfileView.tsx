"use client";

import Link from "next/link";
import { CustomerGuard } from "@/components/layout/AdminGuard";
import { Button, buttonClasses } from "@/components/ui/Button";
import { Panel } from "@/components/ui/Feedback";
import { useLogout } from "@/hooks/useLogout";
import { useAuthStore } from "@/stores/auth/authStore";

function Profile() {
  const user = useAuthStore((state) => state.user);
  const logout = useLogout("/");
  if (!user) return null;

  return (
    <div className="mx-auto max-w-lg space-y-3">
      <h1 className="text-lg font-semibold text-ink">My profile</h1>
      <Panel>
        <dl className="divide-y divide-line text-[13px]">
          {[
            ["Name", user.fullName],
            ["Email", user.email],
            ["Mobile number", user.phone ?? "Not provided"],
          ].map(([label, value]) => (
            <div key={label} className="flex justify-between gap-3 px-4 py-3">
              <dt className="text-ink-muted">{label}</dt>
              <dd className="text-ink">{value}</dd>
            </div>
          ))}
        </dl>
      </Panel>
      <div className="flex gap-2">
        <Link href="/orders" className={buttonClasses("secondary")}>
          View my orders
        </Link>
        <Button variant="ghost" onClick={() => void logout()}>
          Log out
        </Button>
      </div>
    </div>
  );
}

export function ProfileView() {
  return (
    <CustomerGuard>
      <Profile />
    </CustomerGuard>
  );
}
