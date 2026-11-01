"use client";

import { AnimatePresence, motion } from "framer-motion";
import { X } from "lucide-react";
import { useId, useRef, useSyncExternalStore, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { useDialog } from "@/hooks/useDialog";

const subscribeToNothing = () => () => {};

/**
 * Renders into <body> so parent styles can't reach the overlay: `space-y-*` margins shrink a
 * `fixed inset-0` element, and an ancestor's transform would change what it is positioned against.
 */
function OverlayPortal({ children }: { children: ReactNode }) {
  const isClient = useSyncExternalStore(
    subscribeToNothing,
    () => true,
    () => false,
  );
  return isClient ? createPortal(children, document.body) : null;
}

interface OverlayProps {
  open: boolean;
  title: string;
  onClose: () => void;
  footer?: ReactNode;
  children: ReactNode;
}

function OverlayHeader({ id, title, onClose }: { id: string; title: string; onClose: () => void }) {
  return (
    <header className="flex items-center justify-between gap-3 border-b border-line px-4 py-3">
      <h2 id={id} className="text-[15px] font-semibold text-ink">
        {title}
      </h2>
      <button
        type="button"
        aria-label="Close"
        onClick={onClose}
        className="rounded-sm p-1 text-ink-muted hover:bg-canvas hover:text-ink"
      >
        <X aria-hidden className="size-4" />
      </button>
    </header>
  );
}

function Backdrop({ onClose }: { onClose: () => void }) {
  return (
    <motion.div
      aria-hidden
      className="fixed inset-0 z-40 bg-navy-950/40"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.15 }}
      onClick={onClose}
    />
  );
}

function ModalPanel({ title, onClose, footer, children }: Omit<OverlayProps, "open">) {
  const ref = useRef<HTMLDivElement>(null);
  const titleId = useId();
  useDialog(ref, true, onClose);
  return (
    <motion.div
      ref={ref}
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      tabIndex={-1}
      className="fixed top-1/2 left-1/2 z-50 flex max-h-[85vh] w-[calc(100%-2rem)] max-w-lg flex-col rounded-lg border border-line bg-surface shadow-overlay"
      initial={{ opacity: 0, x: "-50%", y: "-48%" }}
      animate={{ opacity: 1, x: "-50%", y: "-50%" }}
      exit={{ opacity: 0, x: "-50%", y: "-48%" }}
      transition={{ duration: 0.15 }}
    >
      <OverlayHeader id={titleId} title={title} onClose={onClose} />
      <div className="overflow-y-auto p-4">{children}</div>
      {footer && (
        <footer className="flex justify-end gap-2 border-t border-line px-4 py-3">{footer}</footer>
      )}
    </motion.div>
  );
}

function DrawerPanel({ title, onClose, footer, children }: Omit<OverlayProps, "open">) {
  const ref = useRef<HTMLDivElement>(null);
  const titleId = useId();
  useDialog(ref, true, onClose);
  return (
    <motion.div
      ref={ref}
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      tabIndex={-1}
      className="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col border-l border-line bg-surface shadow-overlay"
      initial={{ x: "100%" }}
      animate={{ x: 0 }}
      exit={{ x: "100%" }}
      transition={{ duration: 0.2, ease: "easeOut" }}
    >
      <OverlayHeader id={titleId} title={title} onClose={onClose} />
      <div className="flex-1 overflow-y-auto p-4">{children}</div>
      {footer && (
        <footer className="flex justify-end gap-2 border-t border-line px-4 py-3">{footer}</footer>
      )}
    </motion.div>
  );
}

export function Modal({ open, ...panel }: OverlayProps) {
  return (
    <OverlayPortal>
      <AnimatePresence>
        {open && <Backdrop key="backdrop" onClose={panel.onClose} />}
        {open && <ModalPanel key="panel" {...panel} />}
      </AnimatePresence>
    </OverlayPortal>
  );
}

export function Drawer({ open, ...panel }: OverlayProps) {
  return (
    <OverlayPortal>
      <AnimatePresence>
        {open && <Backdrop key="backdrop" onClose={panel.onClose} />}
        {open && <DrawerPanel key="panel" {...panel} />}
      </AnimatePresence>
    </OverlayPortal>
  );
}
