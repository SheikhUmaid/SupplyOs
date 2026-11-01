"use client";

import { RouteError } from "@/components/layout/RouteError";

export default function Error(props: { error: unknown; retry: () => void }) {
  return <RouteError {...props} />;
}
