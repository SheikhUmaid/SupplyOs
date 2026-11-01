import { Suspense } from "react";
import { AdminGuard } from "@/components/layout/AdminGuard";
import { AdminSidebar } from "@/components/layout/AdminSidebar";
import { AdminTopbar } from "@/components/layout/AdminTopbar";

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return (
    // The guard and navigation read the URL, which dynamic routes only know at request time.
    <Suspense>
      <AdminGuard>
        <div className="flex min-h-screen">
          <AdminSidebar />
          <div className="flex min-w-0 flex-1 flex-col">
            <AdminTopbar />
            <main className="flex-1 p-4 lg:p-5">{children}</main>
          </div>
        </div>
      </AdminGuard>
    </Suspense>
  );
}
