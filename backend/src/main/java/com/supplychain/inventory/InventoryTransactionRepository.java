package com.supplychain.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Append-only: nothing in the application updates or deletes ledger rows. */
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    @Query(value = """
            SELECT t FROM InventoryTransaction t
            JOIN FETCH t.inventory i
            JOIN FETCH i.product p
            JOIN FETCH i.warehouse w
            LEFT JOIN FETCH t.performedBy
            LEFT JOIN FETCH t.order
            WHERE (:inventoryId IS NULL OR i.id = :inventoryId)
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
              AND (:type IS NULL OR t.type = :type)
              AND (:q IS NULL OR LOWER(p.name) LIKE :q ESCAPE '!' OR LOWER(p.sku) LIKE :q ESCAPE '!')""",
            countQuery = """
            SELECT COUNT(t) FROM InventoryTransaction t
            JOIN t.inventory i
            JOIN i.product p
            JOIN i.warehouse w
            WHERE (:inventoryId IS NULL OR i.id = :inventoryId)
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
              AND (:type IS NULL OR t.type = :type)
              AND (:q IS NULL OR LOWER(p.name) LIKE :q ESCAPE '!' OR LOWER(p.sku) LIKE :q ESCAPE '!')""")
    Page<InventoryTransaction> search(
            @Param("inventoryId") Long inventoryId,
            @Param("warehouseId") Long warehouseId,
            @Param("type") InventoryTransactionType type,
            @Param("q") String q,
            Pageable pageable);
}
