export interface Crumb {
  label: string;
  href?: string;
}

export interface AdminPage {
  title: string;
  crumbs: Crumb[];
}

export const ADMIN_NAV = [
  { href: "/admin", label: "Overview" },
  { href: "/admin/products", label: "Products" },
  { href: "/admin/warehouses", label: "Warehouses" },
  { href: "/admin/inventory", label: "Inventory" },
  { href: "/admin/orders", label: "Orders" },
  { href: "/admin/dispatch", label: "Dispatch" },
  { href: "/admin/analytics", label: "Analytics" },
] as const;

const ROOT: Crumb = { label: "Control Tower", href: "/admin" };

const SECTION_LABELS: Record<string, string> = {
  ...Object.fromEntries(ADMIN_NAV.map((item) => [item.href, item.label])),
  "/admin/settings": "Settings",
};

export function isNavItemActive(href: string, pathname: string): boolean {
  return href === "/admin"
    ? pathname === "/admin"
    : pathname === href || pathname.startsWith(`${href}/`);
}

/** Resolves the top bar's title and breadcrumbs from the current admin path. */
export function resolveAdminPage(pathname: string): AdminPage {
  const path = pathname.length > 1 ? pathname.replace(/\/$/, "") : pathname;
  if (path === "/admin") return { title: "Overview", crumbs: [{ label: "Control Tower" }] };

  const sectionLabel = SECTION_LABELS[path];
  if (sectionLabel) return { title: sectionLabel, crumbs: [ROOT, { label: sectionLabel }] };

  if (path === "/admin/products/new") return childPage("/admin/products", "New product");
  if (/^\/admin\/orders\/[^/]+$/.test(path)) return childPage("/admin/orders", "Order details");

  return { title: "Control Tower", crumbs: [ROOT] };
}

function childPage(sectionHref: string, title: string): AdminPage {
  const label = SECTION_LABELS[sectionHref] ?? title;
  return { title, crumbs: [ROOT, { label, href: sectionHref }, { label: title }] };
}

const SEARCHABLE_SECTIONS = [
  "/admin/products",
  "/admin/inventory",
  "/admin/orders",
  "/admin/dispatch",
];

/** The top bar searches the section in view, falling back to orders elsewhere. */
export function resolveAdminSearchHref(pathname: string, query: string): string {
  const section =
    SEARCHABLE_SECTIONS.find((href) => isNavItemActive(href, pathname)) ?? "/admin/orders";
  return `${section}?q=${encodeURIComponent(query.trim())}`;
}
