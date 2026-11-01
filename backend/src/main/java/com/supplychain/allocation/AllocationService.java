package com.supplychain.allocation;

import com.supplychain.allocation.AllocationDtos.AllocationResponse;
import com.supplychain.allocation.AllocationDtos.AllocationSettingsResponse;
import com.supplychain.allocation.AllocationDtos.AllocationSummaryResponse;
import com.supplychain.allocation.AllocationDtos.EvaluationResponse;
import com.supplychain.allocation.AllocationDtos.WeightsResponse;
import com.supplychain.allocation.engine.WarehouseScoringEngine;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Candidate;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Decision;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Evaluation;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Line;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Parameters;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Request;
import com.supplychain.allocation.engine.WarehouseScoringEngine.Weights;
import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.dispatch.DispatchService;
import com.supplychain.exception.ApiException;
import com.supplychain.inventory.Inventory;
import com.supplychain.inventory.InventoryService;
import com.supplychain.order.Order;
import com.supplychain.order.OrderItem;
import com.supplychain.order.OrderPriority;
import com.supplychain.order.OrderStatus;
import com.supplychain.order.PaymentStatus;
import com.supplychain.warehouse.Warehouse;
import com.supplychain.warehouse.WarehouseRepository;
import com.supplychain.warehouse.WarehouseStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties(AllocationProperties.class)
public class AllocationService {

    private static final int MAX_REASON_LENGTH = 500;

    private final AllocationProperties properties;
    private final WarehouseScoringEngine engine;
    private final WarehouseAllocationRepository allocations;
    private final AllocationEvaluationRepository evaluations;
    private final WarehouseRepository warehouses;
    private final InventoryService inventoryService;
    private final DispatchService dispatchService;
    private final Clock clock;

    public AllocationService(
            AllocationProperties properties,
            WarehouseAllocationRepository allocations,
            AllocationEvaluationRepository evaluations,
            WarehouseRepository warehouses,
            InventoryService inventoryService,
            DispatchService dispatchService,
            Clock clock) {
        this.properties = properties;
        this.engine = new WarehouseScoringEngine(
                new Parameters(properties.weights(), properties.roadFactor(), properties.speedKmh()));
        this.allocations = allocations;
        this.evaluations = evaluations;
        this.warehouses = warehouses;
        this.inventoryService = inventoryService;
        this.dispatchService = dispatchService;
        this.clock = clock;
    }

    /**
     * Decides and reserves within the caller's transaction, so a failure anywhere undoes all of it.
     *
     * <p>Stock rows are locked BEFORE deciding, in one fixed order. A competing order waits here, then
     * sees the updated stock. The decision can't rest on figures that change underneath it, and two
     * orders can't deadlock by locking the same rows in opposite orders.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public WarehouseAllocation allocate(Order order, List<OrderItem> items) {
        List<Long> productIds = items.stream().map(item -> item.getProduct().getId()).sorted().toList();
        List<Inventory> lockedStock = inventoryService.lockForProducts(productIds);

        Decision decision = engine.evaluate(toRequest(order, items), toCandidates(lockedStock));
        Warehouse selected = decision.selectedWarehouseId().map(warehouses::getReferenceById).orElse(null);
        Instant now = clock.instant();

        Weights weights = properties.weights();
        WarehouseAllocation allocation = allocations.save(new WarehouseAllocation(
                order,
                (int) allocations.countByOrderId(order.getId()) + 1,
                selected == null ? AllocationOutcome.NO_ELIGIBLE_WAREHOUSE : AllocationOutcome.ALLOCATED,
                selected,
                weights.distance(),
                weights.cost(),
                weights.inventory(),
                weights.priority(),
                decision.explanation(),
                now));
        decision.evaluations().forEach(evaluation -> evaluations.save(toEntity(allocation, evaluation)));

        if (selected != null) {
            reserve(order, items, lockedStock, selected.getId(), now);
            order.setSelectedWarehouse(selected);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.PAID_SIMULATED);
            order.setConfirmedAt(now);
            BigDecimal etaHours = decision.evaluations().getFirst().etaHours();
            dispatchService.createFor(order, selected, etaHours, now);
        }
        return allocation;
    }

    private void reserve(Order order, List<OrderItem> items, List<Inventory> lockedStock, long warehouseId, Instant now) {
        for (OrderItem item : items) {
            Inventory row = lockedStock.stream()
                    .filter(stock -> stock.getWarehouse().getId() == warehouseId
                            && stock.getProduct().getId().equals(item.getProduct().getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("The selected warehouse has no stock row"));
            inventoryService.reserve(row, order, item.getQuantity(), now);
        }
    }

    private static Request toRequest(Order order, List<OrderItem> items) {
        List<Line> lines = items.stream()
                .map(item -> new Line(item.getProduct().getId(), item.getProduct().getName(), item.getQuantity()))
                .toList();
        return new Request(
                order.getDeliveryLatitude().doubleValue(),
                order.getDeliveryLongitude().doubleValue(),
                order.getPriority() == OrderPriority.EXPRESS,
                lines);
    }

    /** Every warehouse is evaluated, including inactive ones, so the record shows why each was ruled out. */
    private List<Candidate> toCandidates(List<Inventory> lockedStock) {
        Map<Long, Map<Long, Integer>> stockByWarehouse = new HashMap<>();
        for (Inventory row : lockedStock) {
            stockByWarehouse
                    .computeIfAbsent(row.getWarehouse().getId(), id -> new HashMap<>())
                    .put(row.getProduct().getId(), row.getAvailableQuantity());
        }
        return warehouses.findAll(Sort.by("id")).stream()
                .map(warehouse -> new Candidate(
                        warehouse.getId(),
                        warehouse.getName(),
                        warehouse.getStatus() == WarehouseStatus.ACTIVE,
                        warehouse.getLatitude().doubleValue(),
                        warehouse.getLongitude().doubleValue(),
                        warehouse.getBaseDeliveryCost(),
                        warehouse.getCostPerKm(),
                        warehouse.getHandlingHours(),
                        stockByWarehouse.getOrDefault(warehouse.getId(), Map.of())))
                .toList();
    }

    private AllocationEvaluation toEntity(WarehouseAllocation allocation, Evaluation evaluation) {
        String reason = evaluation.rejectionReason();
        return new AllocationEvaluation(
                allocation,
                warehouses.getReferenceById(evaluation.warehouseId()),
                evaluation.eligible(),
                reason != null && reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH) : reason,
                evaluation.distanceKm(),
                evaluation.deliveryCost(),
                evaluation.etaHours(),
                evaluation.normDistance(),
                evaluation.normCost(),
                evaluation.inventoryFactor(),
                evaluation.priorityFactor(),
                evaluation.finalScore(),
                evaluation.rank(),
                evaluation.decision());
    }

    @Transactional(readOnly = true)
    public PageResponse<AllocationSummaryResponse> list(AllocationOutcome outcome, PageParams params) {
        Sort newestFirst = Sort.by(Sort.Order.desc("allocatedAt"), Sort.Order.desc("id"));
        return PageResponse.from(allocations.search(outcome, params.toPageable(Map.of(), newestFirst))
                .map(allocation -> new AllocationSummaryResponse(
                        allocation.getId(),
                        allocation.getOrder().getId(),
                        allocation.getOrder().getOrderNumber(),
                        allocation.getAttemptNo(),
                        allocation.getOutcome(),
                        allocation.getSelectedWarehouse() == null ? null : allocation.getSelectedWarehouse().getName(),
                        allocation.getAllocatedAt())));
    }

    @Transactional(readOnly = true)
    public AllocationResponse latestForOrder(long orderId) {
        WarehouseAllocation allocation = findLatest(orderId);
        return new AllocationResponse(
                allocation.getId(),
                allocation.getOrder().getId(),
                allocation.getOrder().getOrderNumber(),
                allocation.getAttemptNo(),
                allocation.getOutcome(),
                allocation.getSelectedWarehouse() == null ? null : allocation.getSelectedWarehouse().getName(),
                new WeightsResponse(
                        allocation.getWeightDistance(),
                        allocation.getWeightCost(),
                        allocation.getWeightInventory(),
                        allocation.getWeightPriority()),
                allocation.getExplanation(),
                allocation.getAllocatedAt(),
                allocation.getOrder().getDeliveryCity(),
                allocation.getOrder().getDeliveryLatitude(),
                allocation.getOrder().getDeliveryLongitude(),
                evaluationsOf(allocation));
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> latestEvaluations(long orderId) {
        return evaluationsOf(findLatest(orderId));
    }

    public AllocationSettingsResponse settings() {
        Weights weights = properties.weights();
        return new AllocationSettingsResponse(
                new WeightsResponse(weights.distance(), weights.cost(), weights.inventory(), weights.priority()),
                properties.roadFactor(),
                properties.speedKmh());
    }

    private WarehouseAllocation findLatest(long orderId) {
        return allocations.findLatestByOrderId(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This order has no allocation decision."));
    }

    private List<EvaluationResponse> evaluationsOf(WarehouseAllocation allocation) {
        return evaluations.findWithWarehouseByAllocationId(allocation.getId()).stream()
                .map(EvaluationResponse::from)
                .toList();
    }
}
