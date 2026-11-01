// @vitest-environment node
import { NextRequest } from "next/server";
import { describe, expect, it } from "vitest";
import { AUTH_COOKIE } from "@/lib/authRoutes";
import { proxy } from "./proxy";

describe("proxy", () => {
  it("sends logged-out visitors to login and remembers where they were going", () => {
    const response = proxy(new NextRequest("http://localhost:3000/admin/orders?status=PENDING"));
    const location = new URL(response.headers.get("location")!);

    expect(location.pathname).toBe("/login");
    expect(location.searchParams.get("next")).toBe("/admin/orders?status=PENDING");
  });

  it("lets a request with a session cookie through", () => {
    const response = proxy(
      new NextRequest("http://localhost:3000/admin", {
        headers: { cookie: `${AUTH_COOKIE}=token` },
      }),
    );

    expect(response.headers.get("location")).toBeNull();
    expect(response.headers.get("x-middleware-next")).toBe("1");
  });
});
