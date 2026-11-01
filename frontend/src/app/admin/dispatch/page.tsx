import type { Metadata } from "next";
import { Suspense } from "react";
import { DispatchBoard } from "@/components/dispatch/DispatchBoard";
import { NetworkMap } from "@/components/visualization/NetworkMap";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export const metadata: Metadata = { title: "Dispatch" };

export default function AdminDispatchPage() {
  return (
    <div className="space-y-3">
      <NetworkMap />
      <Suspense>
        <DispatchBoard />
      </Suspense>
    </div>
  );
}
