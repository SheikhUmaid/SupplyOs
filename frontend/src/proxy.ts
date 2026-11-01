import { NextResponse, type NextRequest } from "next/server";
import { AUTH_COOKIE } from "@/lib/authRoutes";

/**
 * A convenience redirect that avoids flashing protected pages at logged-out visitors. It only sees
 * that a cookie exists. The backend verifies the token and the role on every API call.
 */
export function proxy(request: NextRequest) {
  if (request.cookies.has(AUTH_COOKIE)) return NextResponse.next();

  const { pathname, search } = request.nextUrl;
  const login = new URL("/login", request.url);
  login.searchParams.set("next", `${pathname}${search}`);
  return NextResponse.redirect(login);
}

export const config = {
  matcher: ["/admin/:path*", "/checkout", "/orders/:path*", "/profile"],
};
