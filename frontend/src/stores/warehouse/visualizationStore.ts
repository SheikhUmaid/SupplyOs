import { create } from "zustand";

interface VisualizationState {
  selectedWarehouseId: number | null;
  selectedShipmentId: number | null;
  selectWarehouse: (id: number | null) => void;
  selectShipment: (id: number | null) => void;
  clearSelection: () => void;
}

/** What the map has selected. The map and its side panel stay in step through this store. */
export const useVisualizationStore = create<VisualizationState>()((set) => ({
  selectedWarehouseId: null,
  selectedShipmentId: null,
  selectWarehouse: (id) => set({ selectedWarehouseId: id, selectedShipmentId: null }),
  selectShipment: (id) => set({ selectedShipmentId: id, selectedWarehouseId: null }),
  clearSelection: () => set({ selectedWarehouseId: null, selectedShipmentId: null }),
}));
