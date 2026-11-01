package com.supplychain.visualization;

import com.supplychain.common.WarehouseCount;
import com.supplychain.dispatch.Dispatch;
import com.supplychain.dispatch.DispatchRepository;
import com.supplychain.dispatch.ShipmentStatus;
import com.supplychain.inventory.InventoryRepository;
import com.supplychain.inventory.InventoryRepository.WarehouseStock;
import com.supplychain.order.Order;
import com.supplychain.order.OrderRepository;
import com.supplychain.order.OrderStatus;
import com.supplychain.visualization.VisualizationDtos.NetworkResponse;
import com.supplychain.visualization.VisualizationDtos.NetworkShipmentResponse;
import com.supplychain.visualization.VisualizationDtos.NetworkWarehouseResponse;
import com.supplychain.warehouse.Warehouse;
import com.supplychain.warehouse.WarehouseRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The map's data, read from the same tables as every other screen. Nothing here is decorative. */
@Service
public class VisualizationService {

    /** Enough for the map to stay readable. Delivered shipments drop off as newer ones arrive. */
    private static final int MAX_SHIPMENTS = 60;

    private static final Set<OrderStatus> ACTIVE_ORDERS =
            Set.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.DISPATCHED);
    private static final Set<ShipmentStatus> ACTIVE_SHIPMENTS =
            Set.of(ShipmentStatus.PENDING, ShipmentStatus.PACKED, ShipmentStatus.DISPATCHED, ShipmentStatus.IN_TRANSIT);
    private static final Set<ShipmentStatus> SHOWN_SHIPMENTS = Set.of(
            ShipmentStatus.PENDING,
            ShipmentStatus.PACKED,
            ShipmentStatus.DISPATCHED,
            ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.DELIVERED);

    private final WarehouseRepository warehouses;
    private final InventoryRepository inventory;
    private final OrderRepository orders;
    private final DispatchRepository dispatches;

    public VisualizationService(
            WarehouseRepository warehouses,
            InventoryRepository inventory,
            OrderRepository orders,
            DispatchRepository dispatches) {
        this.warehouses = warehouses;
        this.inventory = inventory;
        this.orders = orders;
        this.dispatches = dispatches;
    }

    @Transactional(readOnly = true)
    public NetworkResponse network() {
        List<Warehouse> all = warehouses.findAll(Sort.by("id"));
        List<Long> ids = all.stream().map(Warehouse::getId).toList();
        Map<Long, WarehouseStock> stock = ids.isEmpty()
                ? Map.of()
                : inventory.stockForWarehouses(ids).stream()
                        .collect(Collectors.toMap(WarehouseStock::warehouseId, Function.identity()));
        Map<Long, Long> inStock = counts(inventory.countInStockProducts());
        Map<Long, Long> activeOrders = counts(orders.countByWarehouse(ACTIVE_ORDERS));
        Map<Long, Long> activeDispatches = counts(dispatches.countByWarehouse(ACTIVE_SHIPMENTS));

        return new NetworkResponse(
                all.stream()
                        .map(warehouse -> toWarehouse(
                                warehouse,
                                stock.get(warehouse.getId()),
                                inStock.getOrDefault(warehouse.getId(), 0L),
                                activeOrders.getOrDefault(warehouse.getId(), 0L),
                                activeDispatches.getOrDefault(warehouse.getId(), 0L)))
                        .toList(),
                dispatches
                        .findForMap(
                                SHOWN_SHIPMENTS,
                                PageRequest.of(0, MAX_SHIPMENTS, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                        .stream()
                        .map(VisualizationService::toShipment)
                        .toList());
    }

    private static Map<Long, Long> counts(List<WarehouseCount> rows) {
        return rows.stream().collect(Collectors.toMap(WarehouseCount::warehouseId, WarehouseCount::count));
    }

    private static NetworkWarehouseResponse toWarehouse(
            Warehouse warehouse, WarehouseStock stock, long inStock, long activeOrders, long activeDispatches) {
        return new NetworkWarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getCity(),
                warehouse.getState(),
                warehouse.getLatitude(),
                warehouse.getLongitude(),
                warehouse.getStatus(),
                stock == null ? 0 : stock.productCount(),
                inStock,
                stock == null ? 0 : stock.available(),
                stock == null ? 0 : stock.reserved(),
                stock == null ? 0 : stock.lowStock(),
                activeOrders,
                activeDispatches);
    }

    private static NetworkShipmentResponse toShipment(Dispatch dispatch) {
        Order order = dispatch.getOrder();
        return new NetworkShipmentResponse(
                dispatch.getId(),
                dispatch.getTrackingReference(),
                order.getId(),
                order.getOrderNumber(),
                dispatch.getWarehouse().getId(),
                dispatch.getWarehouse().getName(),
                order.getDeliveryCity(),
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude(),
                dispatch.getStatus(),
                dispatch.getEstimatedDeliveryDate());
    }
}
