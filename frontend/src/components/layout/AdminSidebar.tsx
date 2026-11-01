"use client";

import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import {
  BarChart3,
  Boxes,
  LayoutDashboard,
  LogOut,
  Package,
  PanelLeftClose,
  PanelLeftOpen,
  Settings,
  ShoppingCart,
  Truck,
  UserRound,
  Warehouse,
  type LucideIcon,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect } from "react";
import { useLogout } from "@/hooks/useLogout";
import { cn } from "@/lib/cn";
import { ADMIN_NAV, isNavItemActive } from "@/lib/navigation";
import { useAuthStore } from "@/stores/auth/authStore";
import { useUiStore } from "@/stores/ui/uiStore";
import { Logo } from "./Logo";

const ICONS: Record<(typeof ADMIN_NAV)[number]["href"], LucideIcon> = {
  "/admin": LayoutDashboard,
  "/admin/products": Package,
  "/admin/warehouses": Warehouse,
  "/admin/inventory": Boxes,
  "/admin/orders": ShoppingCart,
  "/admin/dispatch": Truck,
  "/admin/analytics": BarChart3,
};

const ITEM_CLASSES =
  "flex h-9 w-full items-center gap-3 rounded-md px-2.5 text-[13px] font-medium transition-colors";
const IDLE_CLASSES = "text-navy-200 hover:bg-navy-800 hover:text-white";

function SidebarContent({ collapsed, onToggle }: { collapsed: boolean; onToggle?: () => void }) {
  const pathname = usePathname();
  const user = useAuthStore((state) => state.user);
  const logout = useLogout("/login");
  const CollapseIcon = collapsed ? PanelLeftOpen : PanelLeftClose;
  const settingsActive = isNavItemActive("/admin/settings", pathname);

  return (
    <div className="flex h-full flex-col bg-navy-900">
      <div className={cn("flex h-14 shrink-0 items-center px-3.5", collapsed && "justify-center")}>
        <Link href="/admin" aria-label="SupplyChain OS control tower">
          <Logo inverted showWordmark={!collapsed} />
        </Link>
      </div>

      <nav aria-label="Control tower" className="flex-1 space-y-0.5 overflow-y-auto px-2.5 py-2">
        {ADMIN_NAV.map(({ href, label }) => {
          const Icon = ICONS[href];
          const active = isNavItemActive(href, pathname);
          return (
            <Link
              key={href}
              href={href}
              title={collapsed ? label : undefined}
              aria-label={collapsed ? label : undefined}
              aria-current={active ? "page" : undefined}
              className={cn(ITEM_CLASSES, active ? "bg-primary text-white" : IDLE_CLASSES)}
            >
              <Icon aria-hidden className="size-4 shrink-0" strokeWidth={1.75} />
              {!collapsed && label}
            </Link>
          );
        })}
      </nav>

      <div className="space-y-0.5 border-t border-navy-700 p-2.5">
        <div
          title={collapsed ? (user?.fullName ?? "Administrator") : undefined}
          className={cn(ITEM_CLASSES, "text-navy-200")}
        >
          <UserRound aria-hidden className="size-4 shrink-0" strokeWidth={1.75} />
          {!collapsed && <span className="truncate">{user?.fullName ?? "Administrator"}</span>}
        </div>
        <Link
          href="/admin/settings"
          title={collapsed ? "Settings" : undefined}
          aria-label={collapsed ? "Settings" : undefined}
          aria-current={settingsActive ? "page" : undefined}
          className={cn(ITEM_CLASSES, settingsActive ? "bg-primary text-white" : IDLE_CLASSES)}
        >
          <Settings aria-hidden className="size-4 shrink-0" strokeWidth={1.75} />
          {!collapsed && "Settings"}
        </Link>
        <button
          type="button"
          title={collapsed ? "Log out" : undefined}
          aria-label={collapsed ? "Log out" : undefined}
          className={cn(ITEM_CLASSES, IDLE_CLASSES)}
          onClick={() => void logout()}
        >
          <LogOut aria-hidden className="size-4 shrink-0" strokeWidth={1.75} />
          {!collapsed && "Log out"}
        </button>
        {onToggle && (
          <button
            type="button"
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            aria-expanded={!collapsed}
            title={collapsed ? "Expand sidebar" : undefined}
            className={cn(ITEM_CLASSES, IDLE_CLASSES)}
            onClick={onToggle}
          >
            <CollapseIcon aria-hidden className="size-4 shrink-0" strokeWidth={1.75} />
            {!collapsed && "Collapse"}
          </button>
        )}
      </div>
    </div>
  );
}

export function AdminSidebar() {
  const collapsed = useUiStore((state) => state.sidebarCollapsed);
  const toggle = useUiStore((state) => state.toggleSidebar);
  const mobileOpen = useUiStore((state) => state.mobileNavOpen);
  const setMobileOpen = useUiStore((state) => state.setMobileNavOpen);
  const pathname = usePathname();
  const reduceMotion = useReducedMotion();
  const duration = reduceMotion ? 0 : 0.18;

  useEffect(() => {
    setMobileOpen(false);
  }, [pathname, setMobileOpen]);

  return (
    <>
      <motion.aside
        className="sticky top-0 hidden h-screen shrink-0 overflow-hidden md:block"
        initial={false}
        animate={{ width: collapsed ? 60 : 228 }}
        transition={{ duration, ease: "easeInOut" }}
      >
        <SidebarContent collapsed={collapsed} onToggle={toggle} />
      </motion.aside>

      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div
              key="backdrop"
              aria-hidden
              className="fixed inset-0 z-40 bg-navy-950/40 md:hidden"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration }}
              onClick={() => setMobileOpen(false)}
            />
            <motion.aside
              key="panel"
              className="fixed inset-y-0 left-0 z-50 w-60 md:hidden"
              initial={{ x: "-100%" }}
              animate={{ x: 0 }}
              exit={{ x: "-100%" }}
              transition={{ duration, ease: "easeOut" }}
            >
              <SidebarContent collapsed={false} />
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </>
  );
}
