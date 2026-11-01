"use client";

import { Search } from "lucide-react";
import { useEffect, useRef, useState } from "react";

const DEBOUNCE_MS = 300;

/** Reports changes after a short pause in typing, so each keystroke doesn't trigger a request. */
export function SearchInput({
  value,
  onChange,
  label,
}: {
  value: string;
  onChange: (value: string) => void;
  label: string;
}) {
  const [text, setText] = useState(value);
  const [lastValue, setLastValue] = useState(value);
  if (value !== lastValue) {
    // The value changed from outside, such as the top bar's search.
    setLastValue(value);
    setText(value);
  }

  const latestOnChange = useRef(onChange);
  useEffect(() => {
    latestOnChange.current = onChange;
  });

  useEffect(() => {
    if (text === value) return;
    const timer = setTimeout(() => latestOnChange.current(text), DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [text, value]);

  return (
    <div className="relative w-full sm:w-64">
      <Search
        aria-hidden
        className="pointer-events-none absolute top-2.5 left-2.5 size-4 text-ink-subtle"
      />
      <input
        type="search"
        aria-label={label}
        placeholder={label}
        value={text}
        onChange={(event) => setText(event.target.value)}
        className="h-9 w-full rounded-md border border-line-strong bg-surface pr-3 pl-8 text-[13px] placeholder:text-ink-subtle"
      />
    </div>
  );
}
