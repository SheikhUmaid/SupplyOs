"use client";

import { AnimatePresence, motion } from "framer-motion";
import { CheckCircle2, Info, X, XCircle } from "lucide-react";
import { useEffect } from "react";
import { useUiStore, type Toast, type ToastTone } from "@/stores/ui/uiStore";

const TOAST_DURATION_MS = 5000;

const ICONS: Record<ToastTone, typeof Info> = {
  success: CheckCircle2,
  error: XCircle,
  info: Info,
};

const ICON_COLOURS: Record<ToastTone, string> = {
  success: "text-success",
  error: "text-danger",
  info: "text-primary",
};

function ToastItem({ toast }: { toast: Toast }) {
  const dismiss = useUiStore((state) => state.dismissToast);
  const Icon = ICONS[toast.tone];

  useEffect(() => {
    const timer = setTimeout(() => dismiss(toast.id), TOAST_DURATION_MS);
    return () => clearTimeout(timer);
  }, [toast.id, dismiss]);

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, x: 16 }}
      transition={{ duration: 0.15 }}
      className="flex items-start gap-2.5 rounded-md border border-line bg-surface p-3 shadow-overlay"
    >
      <Icon aria-hidden className={`mt-0.5 size-4 shrink-0 ${ICON_COLOURS[toast.tone]}`} />
      <p className="flex-1 text-[13px] text-ink">{toast.message}</p>
      <button
        type="button"
        aria-label="Dismiss notification"
        onClick={() => dismiss(toast.id)}
        className="text-ink-subtle hover:text-ink"
      >
        <X aria-hidden className="size-4" />
      </button>
    </motion.div>
  );
}

export function Toaster() {
  const toasts = useUiStore((state) => state.toasts);
  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed right-4 bottom-4 z-[60] flex w-[calc(100%-2rem)] max-w-sm flex-col gap-2"
    >
      <AnimatePresence initial={false}>
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} />
        ))}
      </AnimatePresence>
    </div>
  );
}
