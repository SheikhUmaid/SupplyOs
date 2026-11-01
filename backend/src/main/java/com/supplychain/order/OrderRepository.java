package com.supplychain.order;

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

public interface OrderRepository extends JpaRepository<Order, Long> {

    long countBySelectedWarehouseIdAndStatusNot(long warehouseId, OrderStatus status);

    @Query("""
            SELECT new com.supplychain.common.WarehouseCount(o.selectedWarehouse.id, COUNT(o))
            FROM Order o
            WHERE o.status IN :statuses AND o.selectedWarehouse IS NOT NULL
            GROUP BY o.selectedWarehouse.id""")
    List<WarehouseCount> countByWarehouse(@Param("statuses") Collection<OrderStatus> statuses);

    /** Every change to an existing order takes this lock first, then any stock rows. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") long id);

    Optional<Order> findByCustomerIdAndIdempotencyKey(long customerId, String idempotencyKey);

    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.customer
            LEFT JOIN FETCH o.selectedWarehouse
            WHERE o.id = :id""")
    Optional<Order> findWithPartiesById(@Param("id") long id);

    @Query(value = """
            SELECT o FROM Order o
            JOIN FETCH o.customer c
            LEFT JOIN FETCH o.selectedWarehouse w
            WHERE (:customerId IS NULL OR c.id = :customerId)
              AND (:status IS NULL OR o.status = :status)
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
              AND (:q IS NULL OR LOWER(o.orderNumber) LIKE :q ESCAPE '!'
                   OR LOWER(c.fullName) LIKE :q ESCAPE '!' OR LOWER(c.email) LIKE :q ESCAPE '!')""",
            countQuery = """
            SELECT COUNT(o) FROM Order o
            JOIN o.customer c
            LEFT JOIN o.selectedWarehouse w
            WHERE (:customerId IS NULL OR c.id = :customerId)
              AND (:status IS NULL OR o.status = :status)
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
              AND (:q IS NULL OR LOWER(o.orderNumber) LIKE :q ESCAPE '!'
                   OR LOWER(c.fullName) LIKE :q ESCAPE '!' OR LOWER(c.email) LIKE :q ESCAPE '!')""")
    Page<Order> search(
            @Param("customerId") Long customerId,
            @Param("status") OrderStatus status,
            @Param("warehouseId") Long warehouseId,
            @Param("q") String q,
            Pageable pageable);
}
