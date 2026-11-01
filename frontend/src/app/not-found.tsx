import { FileQuestion } from "lucide-react";
import Link from "next/link";
import { buttonClasses } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/Feedback";

export default function NotFound() {
  return (
    <main className="flex min-h-screen items-center justify-center p-4">
      <EmptyState
        icon={FileQuestion}
        title="Page not found"
        description="This page doesn't exist or isn't available yet."
        action={
          <Link href="/" className={buttonClasses("secondary", "sm")}>
            Go to home
          </Link>
        }
      />
    </main>
  );
}
