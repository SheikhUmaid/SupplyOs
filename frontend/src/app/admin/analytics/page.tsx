import type { Metadata } from "next";
import { AnalyticsView } from "@/components/analytics/AnalyticsView";

// The admin guard shows nothing until it knows who is logged in, so this can't be instant.
export const instant = false;

export const metadata: Metadata = { title: "Analytics" };

export default function AdminAnalyticsPage() {
  return <AnalyticsView />;
}
