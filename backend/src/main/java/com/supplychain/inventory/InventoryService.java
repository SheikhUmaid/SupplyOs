package com.supplychain.inventory;

import com.supplychain.common.PageParams;
import com.supplychain.common.PageResponse;
import com.supplychain.common.SearchTerm;
import com.supplychain.exception.ApiException;
import com.supplychain.inventory.InventoryDtos.InventoryCreateRequest;
import com.supplychain.inventory.InventoryDtos.InventoryResponse;
import com.supplychain.inventory.InventoryDtos.InventoryTransactionResponse;
import com.supplychain.inventory.InventoryDtos.InventoryUpdateRequest;
import com.supplychain.inventory.InventoryDtos.StockChangeType;
import com.supplychain.order.Order;
import com.supplychain.product.Product;
import com.supplychain.product.ProductRepository;
import com.supplychain.user.User;
import com.supplychain.user.UserRepository;
import com.supplychain.warehouse.Warehouse;
import com.supplychain.warehouse.WarehouseRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** The only code that changes stock. Every change locks its row and writes a ledger entry. */
@Service
public class InventoryService {

    public static final String OPENING_STOCK_NOTE = "Opening stock";
    private static final String RECORD_EXISTS = "This product already has a stock record in this warehouse.";
    private static final Map<String, String> SORTABLE = Map.of(
            "product", "product.name",
            "warehouse", "warehouse.name",
            "available", "availableQuantity",
            "reserved", "reservedQuantity",
            "lastUpdated", "lastUpdated");
    private static final Sort DEFAULT_SORT = Sort.by("product.name", "warehouse.name");

    private final InventoryRepository inventory;
    private final InventoryTransactionRepository transactions;
    private final ProductRepository products;
    private final WarehouseRepository warehouses;
    private final UserRepository users;
    private final Clock clock;

    public InventoryService(
            InventoryRepository inventory,
            InventoryTransactionRepository transactions,
            ProductRepository products,
            WarehouseRepository warehouses,
            UserRepository users,
            Clock clock) {
        this.inventory = inventory;
        this.transactions = transactions;
        this.products = products;
        this.warehouses = warehouses;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> list(String q, Long warehouseId, boolean lowStockOnly, PageParams params) {
        return PageResponse.from(inventory
                .search(SearchTerm.contains(q), warehouseId, lowStockOnly, params.toPageable(SORTABLE, DEFAULT_SORT))
                .map(InventoryResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> listTransactions(
            Long inventoryId, Long warehouseId, InventoryTransactionType type, String q, PageParams params) {
        Sort newestFirst = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return PageResponse.from(transactions
                .search(inventoryId, warehouseId, type, SearchTerm.contains(q), params.toPageable(Map.of(), newestFirst))
                .map(InventoryTransactionResponse::from));
    }

    /** A null actor means the system made the change. */
    @Transactional
    public InventoryResponse create(InventoryCreateRequest request, Long actorId) {
        if (inventory.existsByProductIdAndWarehouseId(request.productId(), request.warehouseId())) {
            throw new ApiException(HttpStatus.CONFLICT, RECORD_EXISTS);
        }
        Product product = products.findById(request.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This product doesn't exist."));
        Warehouse warehouse = warehouses.findById(request.warehouseId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This warehouse doesn't exist."));
        Instant now = clock.instant();
        Inventory record = new Inventory(product, warehouse, request.availableQuantity(), 0, request.reorderLevel(), now);
        try {
            inventory.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, RECORD_EXISTS);
        }
        if (request.availableQuantity() > 0) {
            writeLedger(
                    record,
                    InventoryTransactionType.RESTOCK,
                    request.availableQuantity(),
                    OPENING_STOCK_NOTE,
                    actorId == null ? null : users.getReferenceById(actorId),
                    now);
        }
        return InventoryResponse.from(record);
    }

    @Transactional
    public InventoryResponse update(long id, InventoryUpdateRequest request, long actorId) {
        int quantity = request.quantity();
        if (request.type() == StockChangeType.RESTOCK && quantity < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Restocking must add stock. Use an adjustment to remove it.");
        }
        if (quantity == 0 && request.reorderLevel() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a quantity other than zero.");
        }

        Inventory record = inventory.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This stock record doesn't exist."));
        // Checked again here, after the lock, so the decision can't be based on a stale count.
        if (record.getAvailableQuantity() + quantity < 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Only " + record.getAvailableQuantity() + " available, so " + -quantity
                            + " can't be removed. Reserved stock can't be adjusted.");
        }

        Instant now = clock.instant();
        if (request.reorderLevel() != null) {
            record.setReorderLevel(request.reorderLevel());
        }
        record.setLastUpdated(now);
        if (quantity != 0) {
            record.setAvailableQuantity(record.getAvailableQuantity() + quantity);
            InventoryTransactionType type = request.type() == StockChangeType.RESTOCK
                    ? InventoryTransactionType.RESTOCK
                    : InventoryTransactionType.ADJUSTMENT;
            writeLedger(record, type, quantity, request.reason().trim(), users.getReferenceById(actorId), now);
        }
        return InventoryResponse.from(record);
    }

    /** The rows stay locked until the caller's transaction ends. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Inventory> lockForProducts(Collection<Long> productIds) {
        return inventory.findForUpdateByProductIds(productIds);
    }

    /** `row` must already be locked. Moves units from available to reserved. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(Inventory row, Order order, int quantity, Instant at) {
        if (row.getAvailableQuantity() < quantity) {
            throw new IllegalStateException("Reservation exceeds available stock for inventory " + row.getId());
        }
        moveForOrder(row, order, InventoryTransactionType.RESERVE, -quantity, quantity, at);
    }

    /** `row` must already be locked. Returns units from reserved to available. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Inventory row, Order order, int quantity, Instant at) {
        if (row.getReservedQuantity() < quantity) {
            throw new IllegalStateException("Release exceeds reserved stock for inventory " + row.getId());
        }
        moveForOrder(row, order, InventoryTransactionType.RELEASE, quantity, -quantity, at);
    }

    /** `row` must already be locked. The goods have left, so the units stop being reserved. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void ship(Inventory row, Order order, int quantity, Instant at) {
        if (row.getReservedQuantity() < quantity) {
            throw new IllegalStateException("Shipment exceeds reserved stock for inventory " + row.getId());
        }
        moveForOrder(row, order, InventoryTransactionType.SHIP, 0, -quantity, at);
    }

    private void moveForOrder(
            Inventory row, Order order, InventoryTransactionType type, int availableDelta, int reservedDelta, Instant at) {
        row.setAvailableQuantity(row.getAvailableQuantity() + availableDelta);
        row.setReservedQuantity(row.getReservedQuantity() + reservedDelta);
        row.setLastUpdated(at);
        transactions.save(new InventoryTransaction(
                row,
                order,
                null,
                type,
                availableDelta,
                reservedDelta,
                row.getAvailableQuantity(),
                row.getReservedQuantity(),
                null,
                at));
    }

    private void writeLedger(
            Inventory record, InventoryTransactionType type, int availableDelta, String note, User actor, Instant at) {
        transactions.save(new InventoryTransaction(
                record,
                null,
                actor,
                type,
                availableDelta,
                0,
                record.getAvailableQuantity(),
                record.getReservedQuantity(),
                note,
                at));
    }
}
