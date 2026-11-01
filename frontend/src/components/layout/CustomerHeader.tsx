"use client";

import { Search, ShoppingCart, UserRound } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Dropdown } from "@/components/ui/Dropdown";
import { useLogout } from "@/hooks/useLogout";
import { cartUnitCount } from "@/stores/cart/cartCalculations";
import { useCartStore } from "@/stores/cart/cartStore";
import { useAuthStore } from "@/stores/auth/authStore";
import { Logo } from "./Logo";

const ICON_BUTTON =
  "relative flex size-9 items-center justify-center rounded-md text-ink-muted hover:bg-canvas hover:text-ink";
const MENU_ITEM = "block w-full px-3 py-2 text-left text-ink hover:bg-canvas";

export function CustomerHeader() {
  const router = useRouter();
  const units = useCartStore((state) => cartUnitCount(state.items));
  const user = useAuthStore((state) => state.user);
  const logout = useLogout("/");
  const [query, setQuery] = useState("");

  function onSearch(event: FormEvent) {
    event.preventDefault();
    const trimmed = query.trim();
    router.push(trimmed ? `/products?q=${encodeURIComponent(trimmed)}` : "/products");
  }

  const searchForm = (className: string) => (
    <form role="search" onSubmit={onSearch} className={className}>
      <Search
        aria-hidden
        className="pointer-events-none absolute top-2.5 left-2.5 size-4 text-ink-subtle"
      />
      <input
        type="search"
        aria-label="Search products"
        placeholder="Search products"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        className="h-9 w-full rounded-md border border-line-strong bg-surface-muted pr-3 pl-8 text-[13px] placeholder:text-ink-subtle"
      />
    </form>
  );

  return (
    <header className="sticky top-0 z-20 border-b border-line bg-surface">
      <div className="mx-auto flex h-14 max-w-7xl items-center gap-4 px-4">
        <Link href="/" aria-label="SupplyChain OS home" className="shrink-0">
          <Logo />
        </Link>
        <nav
          aria-label="Shop"
          className="hidden items-center gap-1 text-[13px] font-medium md:flex"
        >
          <Link href="/products" className="rounded-md px-2.5 py-1.5 text-ink-muted hover:text-ink">
            Products
          </Link>
          <Link href="/orders" className="rounded-md px-2.5 py-1.5 text-ink-muted hover:text-ink">
            My orders
          </Link>
        </nav>
        {searchForm("relative mx-auto hidden w-full max-w-md md:block")}
        <div className="ml-auto flex shrink-0 items-center gap-1 md:ml-0">
          <Link
            href="/cart"
            aria-label={
              units > 0 ? `Cart, ${units} ${units === 1 ? "item" : "items"}` : "Cart, empty"
            }
            className={ICON_BUTTON}
          >
            <ShoppingCart aria-hidden className="size-4.5" strokeWidth={1.75} />
            {units > 0 && (
              <span className="absolute top-0.5 right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold text-white tabular-nums">
                {units}
              </span>
            )}
          </Link>
          <Dropdown
            label="Account menu"
            triggerClassName={ICON_BUTTON}
            trigger={<UserRound aria-hidden className="size-4.5" strokeWidth={1.75} />}
          >
            {(close) => (
              <div className="py-1 text-[13px]">
                {user ? (
                  <>
                    <div className="border-b border-line px-3 py-2">
                      <p className="font-medium text-ink">{user.fullName}</p>
                      <p className="text-xs text-ink-muted">{user.email}</p>
                    </div>
                    <Link href="/profile" onClick={close} className={MENU_ITEM}>
                      Profile
                    </Link>
                    <Link href="/orders" onClick={close} className={MENU_ITEM}>
                      My orders
                    </Link>
                    <button
                      type="button"
                      className={MENU_ITEM}
                      onClick={() => {
                        close();
                        void logout();
                      }}
                    >
                      Log out
                    </button>
                  </>
                ) : (
                  <>
                    <Link href="/login" onClick={close} className={MENU_ITEM}>
                      Log in
                    </Link>
                    <Link href="/register" onClick={close} className={MENU_ITEM}>
                      Create account
                    </Link>
                  </>
                )}
              </div>
            )}
          </Dropdown>
        </div>
      </div>
      <div className="px-4 pb-2.5 md:hidden">{searchForm("relative")}</div>
    </header>
  );
}
