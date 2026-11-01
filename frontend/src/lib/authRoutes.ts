import type { UserRole } from "@/types/api";

export const AUTH_COOKIE = "scos_token";

export function homeFor(role: UserRole): string {
  return role === "ADMIN" ? "/admin" : "/";
}

/** Only same-site paths are followed after login, so a crafted link can't redirect off the site. */
export function safeNextPath(next: string | null): string | null {
  if (!next || !next.startsWith("/") || next.startsWith("//") || next.includes("\\")) return null;
  return next;
}
