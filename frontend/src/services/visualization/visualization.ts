import { api } from "@/services/api/client";
import type { Network } from "@/types/api";

export const visualizationService = {
  network: (signal?: AbortSignal) => api.get<Network>("/visualization/network", { signal }),
};
