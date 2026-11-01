package com.supplychain.inventory;

import com.supplychain.common.WarehouseCount;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    record StockTotals(long productId, long available, long reorderLevel) {}

    record WarehouseStock(long warehouseId, long productCount, long available, long reserved, long lowStock) {}

    /** Holds the row until the transaction ends, so concurrent stock changes run one at a time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") long id);

    /**
     * Locks every warehouse's stock of these products until the transaction ends. Always in one fixed
     * order, so that two transactions can't deadlock by taking the same rows in opposite orders.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM Inventory i
            WHERE i.product.id IN :productIds
            ORDER BY i.product.id, i.warehouse.id""")
    List<Inventory> findForUpdateByProductIds(@Param("productIds") Collection<Long> productIds);

    boolean existsByProductIdAndWarehouseId(long productId, long warehouseId);

    /** Low stock counts only products still on sale, in warehouses still operating. */
    @Query(value = """
            SELECT i FROM Inventory i
            JOIN FETCH i.product p
            JOIN FETCH i.warehouse w
            WHERE (:q IS NULL OR LOWER(p.name) LIKE :q ESCAPE '!' OR LOWER(p.sku) LIKE :q ESCAPE '!')
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
              AND (:lowStockOnly = FALSE OR (i.availableQuantity < i.reorderLevel
                   AND p.status = com.supplychain.product.ProductStatus.ACTIVE
                   AND w.status = com.supplychain.warehouse.WarehouseStatus.ACTIVE))""",
            countQuery = """
            SELECT COUNT(i) FROM Inventory i
            JOIN i.product p
            JOIN i.warehouse w
            WHERE (:q IS NULL OR LOWER(p.name) LIKE :q ESCAPE '!' OR LOWER(p.sku) LIKE :q ESCAPE '!')
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
              AND (:lowStockOnly = FALSE OR (i.availableQuantity < i.reorderLevel
                   AND p.status = com.supplychain.product.ProductStatus.ACTIVE
                   AND w.status = com.supplychain.warehouse.WarehouseStatus.ACTIVE))""")
    Page<Inventory> search(
            @Param("q") String q,
            @Param("warehouseId") Long warehouseId,
            @Param("lowStockOnly") boolean lowStockOnly,
            Pageable pageable);

    @Query("""
            SELECT new com.supplychain.common.WarehouseCount(i.warehouse.id, COUNT(i))
            FROM Inventory i
            WHERE i.availableQuantity > 0
            GROUP BY i.warehouse.id""")
    List<WarehouseCount> countInStockProducts();

    /** Inactive warehouses can't fulfil orders, so their stock doesn't count towards availability. */
    @Query("""
            SELECT new com.supplychain.inventory.InventoryRepository$StockTotals(
                i.product.id, SUM(i.availableQuantity), SUM(i.reorderLevel))
            FROM Inventory i
            WHERE i.product.id IN :productIds
              AND i.warehouse.status = com.supplychain.warehouse.WarehouseStatus.ACTIVE
            GROUP BY i.product.id""")
    List<StockTotals> totalsForProducts(@Param("productIds") Collection<Long> productIds);

    @Query("""
            SELECT new com.supplychain.inventory.InventoryRepository$WarehouseStock(
                i.warehouse.id, COUNT(i), SUM(i.availableQuantity), SUM(i.reservedQuantity),
                SUM(CASE WHEN i.availableQuantity < i.reorderLevel
                          AND i.product.status = com.supplychain.product.ProductStatus.ACTIVE
                         THEN 1L ELSE 0L END))
            FROM Inventory i
            WHERE i.warehouse.id IN :warehouseIds
            GROUP BY i.warehouse.id""")
    List<WarehouseStock> stockForWarehouses(@Param("warehouseIds") Collection<Long> warehouseIds);
}
