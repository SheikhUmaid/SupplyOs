import { useSyncExternalStore } from "react";
import { useCartStore } from "@/stores/cart/cartStore";

function subscribe(onChange: () => void): () => void {
  return useCartStore.persist.onFinishHydration(onChange);
}

/** False until the saved cart has loaded, so an empty cart isn't shown or acted on too early. */
export function useCartHydrated(): boolean {
  return useSyncExternalStore(
    subscribe,
    () => useCartStore.persist.hasHydrated(),
    // The persist API doesn't exist during server rendering, which has no localStorage.
    () => false,
  );
}
