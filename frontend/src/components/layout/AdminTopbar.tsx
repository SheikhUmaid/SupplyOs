"use client";

import { Bell, ChevronRight, Menu, Search, UserRound } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Fragment, useState, type FormEvent } from "react";
import { Dropdown } from "@/components/ui/Dropdown";
import { EmptyState } from "@/components/ui/Feedback";
import { useLogout } from "@/hooks/useLogout";
import { resolveAdminPage, resolveAdminSearchHref } from "@/lib/navigation";
import { useAuthStore } from "@/stores/auth/authStore";
import { useUiStore } from "@/stores/ui/uiStore";

const ICON_BUTTON =
  "flex size-9 items-center justify-center rounded-md text-ink-muted hover:bg-canvas hover:text-ink";

export function AdminTopbar() {
  const pathname = usePathname();
  const router = useRouter();
  const { title, crumbs } = resolveAdminPage(pathname);
  const user = useAuthStore((state) => state.user);
  const logout = useLogout("/login");
  const openMobileNav = useUiStore((state) => state.setMobileNavOpen);
  const [query, setQuery] = useState("");

  function onSearch(event: FormEvent) {
    event.preventDefault();
    if (query.trim()) router.push(resolveAdminSearchHref(pathname, query));
  }

  return (
    <header className="sticky top-0 z-20 flex h-14 items-center gap-3 border-b border-line bg-surface px-4">
      <button
        type="button"
        aria-label="Open navigation"
        className={`${ICON_BUTTON} -ml-1.5 md:hidden`}
        onClick={() => openMobileNav(true)}
      >
        <Menu aria-hidden className="size-5" />
      </button>

      <div className="min-w-0 flex-1">
        <h1 className="truncate text-[15px] leading-5 font-semibold text-ink">{title}</h1>
        <nav aria-label="Breadcrumb" className="hidden sm:block">
          <ol className="flex items-center gap-1 text-xs text-ink-muted">
            {crumbs.map((crumb, index) => (
              <Fragment key={crumb.label}>
                {index > 0 && <ChevronRight aria-hidden className="size-3" />}
                <li>
                  {crumb.href ? (
                    <Link href={crumb.href} className="hover:text-primary">
                      {crumb.label}
                    </Link>
                  ) : (
                    <span aria-current="page">{crumb.label}</span>
                  )}
                </li>
              </Fragment>
            ))}
          </ol>
        </nav>
      </div>

      <form role="search" onSubmit={onSearch} className="relative hidden w-64 lg:block">
        <Search
          aria-hidden
          className="pointer-events-none absolute top-2.5 left-2.5 size-4 text-ink-subtle"
        />
        <input
          type="search"
          aria-label="Search"
          placeholder="Search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          className="h-9 w-full rounded-md border border-line-strong bg-surface-muted pr-3 pl-8 text-[13px] placeholder:text-ink-subtle"
        />
      </form>

      <Dropdown
        label="Notifications"
        triggerClassName={ICON_BUTTON}
        trigger={<Bell aria-hidden className="size-4.5" strokeWidth={1.75} />}
      >
        {() => <EmptyState title="No notifications" />}
      </Dropdown>

      <Dropdown
        label="Administrator menu"
        triggerClassName={ICON_BUTTON}
        trigger={<UserRound aria-hidden className="size-4.5" strokeWidth={1.75} />}
      >
        {(close) => (
          <div className="py-1 text-[13px]">
            <div className="border-b border-line px-3 py-2">
              <p className="font-medium text-ink">{user?.fullName ?? "Administrator"}</p>
              {user && <p className="text-xs text-ink-muted">{user.email}</p>}
            </div>
            <Link
              href="/admin/settings"
              onClick={close}
              className="block px-3 py-2 text-ink hover:bg-canvas"
            >
              Settings
            </Link>
            <button
              type="button"
              className="block w-full px-3 py-2 text-left text-ink hover:bg-canvas"
              onClick={() => {
                close();
                void logout();
              }}
            >
              Log out
            </button>
          </div>
        )}
      </Dropdown>
    </header>
  );
}
