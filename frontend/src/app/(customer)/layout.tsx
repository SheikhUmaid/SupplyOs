import { CustomerHeader } from "@/components/layout/CustomerHeader";

export default function CustomerLayout({ children }: LayoutProps<"/">) {
  return (
    <div className="flex min-h-screen flex-col">
      <CustomerHeader />
      <main className="mx-auto w-full max-w-7xl flex-1 p-4">{children}</main>
      <footer className="border-t border-line bg-surface px-4 py-3 text-center text-xs text-ink-muted">
        SupplyChain OS. Academic demonstration: payments and deliveries are simulated.
      </footer>
    </div>
  );
}
