import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AdminGuard } from "@/components/layout/AdminGuard";
import { ApiError } from "@/services/api/client";
import { authService } from "@/services/auth/auth";
import { useAuthStore } from "@/stores/auth/authStore";
import type { CurrentUser } from "@/types/api";
import { LoginForm } from "./LoginForm";

const replace = vi.fn();
let search = "";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn() }),
  usePathname: () => "/admin/orders",
  useSearchParams: () => new URLSearchParams(search),
}));

const admin: CurrentUser = { id: 1, fullName: "A", email: "a@x.co", phone: null, role: "ADMIN" };
const customer: CurrentUser = { ...admin, id: 2, role: "CUSTOMER" };

beforeEach(() => {
  replace.mockReset();
  search = "";
  useAuthStore.setState({ status: "loading", user: null });
  vi.restoreAllMocks();
});

function submit(email: string, password: string) {
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: email } });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Log in" }));
}

describe("LoginForm", () => {
  it("validates before calling the server", () => {
    const login = vi.spyOn(authService, "login");
    render(<LoginForm />);
    submit("not-an-email", "");

    expect(screen.getByText("Enter a valid email address.")).toBeInTheDocument();
    expect(screen.getByText("This field is required.")).toBeInTheDocument();
    expect(login).not.toHaveBeenCalled();
  });

  it("shows the server's message and allows another attempt", async () => {
    vi.spyOn(authService, "login").mockRejectedValue(
      new ApiError(401, "Incorrect email or password."),
    );
    render(<LoginForm />);
    submit("a@x.co", "wrong-password");

    expect(await screen.findByRole("alert")).toHaveTextContent("Incorrect email or password.");
    expect(screen.getByRole("button", { name: "Log in" })).toBeEnabled();
    expect(replace).not.toHaveBeenCalled();
  });

  it.each([
    ["an administrator", admin, "/admin"],
    ["a customer", customer, "/"],
  ])("sends %s to their own interface", async (_, user, target) => {
    vi.spyOn(authService, "login").mockResolvedValue(user);
    render(<LoginForm />);
    submit("a@x.co", "correct-horse");

    await waitFor(() => expect(replace).toHaveBeenCalledWith(target));
  });

  it("returns to the requested page after login", async () => {
    search = "next=/admin/orders";
    vi.spyOn(authService, "login").mockResolvedValue(admin);
    render(<LoginForm />);
    submit("a@x.co", "correct-horse");

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/admin/orders"));
  });

  it("keeps the return path when moving on to registration", () => {
    search = "next=/checkout";
    render(<LoginForm />);
    expect(screen.getByRole("link", { name: "Create an account" })).toHaveAttribute(
      "href",
      "/register?next=%2Fcheckout",
    );
  });

  it("ignores a next link pointing at another site", async () => {
    search = "next=https://evil.example";
    vi.spyOn(authService, "login").mockResolvedValue(customer);
    render(<LoginForm />);
    submit("a@x.co", "correct-horse");

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
  });
});

describe("AdminGuard", () => {
  const renderGuard = () =>
    render(
      <AdminGuard>
        <p>Admin content</p>
      </AdminGuard>,
    );

  it("hides content while the session is being checked", () => {
    renderGuard();
    expect(screen.queryByText("Admin content")).not.toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });

  it("shows content to administrators", () => {
    renderGuard();
    act(() => useAuthStore.getState().setUser(admin));
    expect(screen.getByText("Admin content")).toBeInTheDocument();
  });

  it("sends customers to the shop without showing admin content", () => {
    renderGuard();
    act(() => useAuthStore.getState().setUser(customer));
    expect(screen.queryByText("Admin content")).not.toBeInTheDocument();
    expect(replace).toHaveBeenCalledWith("/");
  });

  it("sends anonymous visitors to login", () => {
    renderGuard();
    act(() => useAuthStore.getState().setUser(null));
    expect(replace).toHaveBeenCalledWith("/login?next=%2Fadmin%2Forders");
  });

  it("removes admin content when the session ends", () => {
    renderGuard();
    act(() => useAuthStore.getState().setUser(admin));
    act(() => useAuthStore.getState().setUser(null));
    expect(screen.queryByText("Admin content")).not.toBeInTheDocument();
  });
});
