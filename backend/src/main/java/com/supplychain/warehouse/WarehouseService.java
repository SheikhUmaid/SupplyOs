package com.supplychain.warehouse;

import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.common.SearchTerm;
import com.supplychain.dispatch.DispatchRepository;
import com.supplychain.dispatch.ShipmentStatus;
import com.supplychain.exception.ApiException;
import com.supplychain.inventory.InventoryRepository;
import com.supplychain.inventory.InventoryRepository.WarehouseStock;
import com.supplychain.order.OrderRepository;
import com.supplychain.order.OrderStatus;
import com.supplychain.warehouse.WarehouseDtos.StockSummaryResponse;
import com.supplychain.warehouse.WarehouseDtos.WarehouseDetailResponse;
import com.supplychain.warehouse.WarehouseDtos.WarehousePerformanceResponse;
import com.supplychain.warehouse.WarehouseDtos.WarehouseRequest;
import com.supplychain.warehouse.WarehouseDtos.WarehouseResponse;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

    private static final Map<String, String> SORTABLE = Map.of("name", "name", "city", "city");
    private static final List<ShipmentStatus> ACTIVE_SHIPMENTS =
            List.of(ShipmentStatus.PENDING, ShipmentStatus.PACKED, ShipmentStatus.DISPATCHED, ShipmentStatus.IN_TRANSIT);

    private final WarehouseRepository warehouses;
    private final InventoryRepository inventory;
    private final OrderRepository orders;
    private final DispatchRepository dispatches;
    private final Clock clock;

    public WarehouseService(
            WarehouseRepository warehouses,
            InventoryRepository inventory,
            OrderRepository orders,
            DispatchRepository dispatches,
            Clock clock) {
        this.warehouses = warehouses;
        this.inventory = inventory;
        this.orders = orders;
        this.dispatches = dispatches;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<WarehouseResponse> list(String q, WarehouseStatus status, PageParams params) {
        Page<Warehouse> page =
                warehouses.search(SearchTerm.contains(q), status, params.toPageable(SORTABLE, Sort.by("name")));
        Map<Long, WarehouseStock> stock = page.isEmpty()
                ? Map.of()
                : inventory.stockForWarehouses(page.map(Warehouse::getId).getContent()).stream()
                        .collect(Collectors.toMap(WarehouseStock::warehouseId, Function.identity()));
        return PageResponse.from(page.map(warehouse ->
                WarehouseResponse.from(warehouse, StockSummaryResponse.from(stock.get(warehouse.getId())))));
    }

    @Transactional(readOnly = true)
    public WarehouseDetailResponse get(long id) {
        Warehouse warehouse = find(id);
        long allocated = orders.countBySelectedWarehouseIdAndStatusNot(id, OrderStatus.CANCELLED);
        long delivered = dispatches.countByWarehouseIdAndStatusIn(id, List.of(ShipmentStatus.DELIVERED));
        long active = dispatches.countByWarehouseIdAndStatusIn(id, ACTIVE_SHIPMENTS);
        return new WarehouseDetailResponse(
                toResponse(warehouse),
                new WarehousePerformanceResponse(
                        allocated, active, delivered, allocated == 0 ? null : (double) delivered / allocated));
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        Warehouse warehouse = new Warehouse(
                request.name().trim(),
                request.address().trim(),
                request.city().trim(),
                request.state().trim(),
                request.postalCode().trim(),
                request.latitude(),
                request.longitude(),
                request.contactNumber().trim(),
                request.baseDeliveryCost(),
                request.costPerKm(),
                request.handlingHours(),
                request.status() == null ? WarehouseStatus.ACTIVE : request.status(),
                clock.instant());
        return save(warehouse);
    }

    @Transactional
    public WarehouseResponse update(long id, WarehouseRequest request) {
        Warehouse warehouse = find(id);
        warehouse.setName(request.name().trim());
        warehouse.setAddress(request.address().trim());
        warehouse.setCity(request.city().trim());
        warehouse.setState(request.state().trim());
        warehouse.setPostalCode(request.postalCode().trim());
        warehouse.setLatitude(request.latitude());
        warehouse.setLongitude(request.longitude());
        warehouse.setContactNumber(request.contactNumber().trim());
        warehouse.setBaseDeliveryCost(request.baseDeliveryCost());
        warehouse.setCostPerKm(request.costPerKm());
        warehouse.setHandlingHours(request.handlingHours());
        if (request.status() != null) {
            warehouse.setStatus(request.status());
        }
        return save(warehouse);
    }

    /** New orders stop considering this warehouse. Stock already reserved here is unaffected. */
    @Transactional
    public void deactivate(long id) {
        find(id).setStatus(WarehouseStatus.INACTIVE);
    }

    private WarehouseResponse save(Warehouse warehouse) {
        try {
            return toResponse(warehouses.saveAndFlush(warehouse));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "A warehouse with this name already exists.");
        }
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        WarehouseStock stock = inventory.stockForWarehouses(List.of(warehouse.getId())).stream()
                .findFirst()
                .orElse(null);
        return WarehouseResponse.from(warehouse, StockSummaryResponse.from(stock));
    }

    private Warehouse find(long id) {
        return warehouses.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This warehouse doesn't exist."));
    }
}
