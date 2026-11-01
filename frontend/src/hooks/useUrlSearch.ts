import { useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { useFilterStore, type FilterSections } from "@/stores/filters/filterStore";

/** Applies the top bar's ?q= search to a list. Components using this need a Suspense boundary. */
export function useUrlSearch(section: keyof FilterSections): void {
  const q = useSearchParams().get("q");
  const setFilters = useFilterStore((state) => state.setFilters);

  useEffect(() => {
    if (q !== null) setFilters(section, { q });
  }, [q, section, setFilters]);
}
