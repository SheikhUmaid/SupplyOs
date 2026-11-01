import { cn } from "@/lib/cn";

export function Logo({
  showWordmark = true,
  inverted = false,
}: {
  showWordmark?: boolean;
  inverted?: boolean;
}) {
  return (
    <span className="inline-flex items-center gap-2">
      <svg aria-hidden viewBox="0 0 32 32" className="size-7 shrink-0">
        <rect width="32" height="32" rx="5" fill="var(--color-primary)" />
        <path d="M9 22 16 10l7 12z" fill="none" stroke="#fff" strokeWidth="2" />
        <circle cx="16" cy="10" r="3" fill="#fff" />
        <circle cx="9" cy="22" r="3" fill="#fff" />
        <circle cx="23" cy="22" r="3" fill="#fff" />
      </svg>
      {showWordmark && (
        <span
          className={cn(
            "text-[15px] font-semibold tracking-tight whitespace-nowrap",
            inverted ? "text-white" : "text-ink",
          )}
        >
          SupplyChain OS
        </span>
      )}
    </span>
  );
}
