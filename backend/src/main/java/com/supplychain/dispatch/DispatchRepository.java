package com.supplychain.dispatch;

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

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {

    long countByWarehouseIdAndStatusIn(long warehouseId, Collection<ShipmentStatus> statuses);

    Optional<Dispatch> findByOrderId(long orderId);

    @Query("""
            SELECT new com.supplychain.common.WarehouseCount(d.warehouse.id, COUNT(d))
            FROM Dispatch d
            WHERE d.status IN :statuses
            GROUP BY d.warehouse.id""")
    List<WarehouseCount> countByWarehouse(@Param("statuses") Collection<ShipmentStatus> statuses);

    @Query("""
            SELECT d FROM Dispatch d
            JOIN FETCH d.order
            JOIN FETCH d.warehouse
            WHERE d.status IN :statuses""")
    List<Dispatch> findForMap(@Param("statuses") Collection<ShipmentStatus> statuses, Pageable pageable);

    /**
     * A locking read always sees the latest committed row. A plain read inside the same transaction
     * could return an old snapshot from before this transaction waited for its locks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Dispatch d WHERE d.id = :id")
    Optional<Dispatch> findByIdForUpdate(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Dispatch d WHERE d.order.id = :orderId")
    Optional<Dispatch> findByOrderIdForUpdate(@Param("orderId") long orderId);

    @Query("SELECT d.order.id FROM Dispatch d WHERE d.id = :id")
    Optional<Long> findOrderIdById(@Param("id") long id);

    @Query("""
            SELECT d FROM Dispatch d
            JOIN FETCH d.order o
            JOIN FETCH o.customer
            JOIN FETCH d.warehouse
            WHERE d.id = :id""")
    Optional<Dispatch> findWithPartiesById(@Param("id") long id);

    @Query(value = """
            SELECT d FROM Dispatch d
            JOIN FETCH d.order o
            JOIN FETCH o.customer c
            JOIN FETCH d.warehouse w
            WHERE (:status IS NULL OR d.status = :status)
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
              AND (:q IS NULL OR LOWER(d.trackingReference) LIKE :q ESCAPE '!'
                   OR LOWER(o.orderNumber) LIKE :q ESCAPE '!' OR LOWER(c.fullName) LIKE :q ESCAPE '!')""",
            countQuery = """
            SELECT COUNT(d) FROM Dispatch d
            JOIN d.order o
            JOIN o.customer c
            JOIN d.warehouse w
            WHERE (:status IS NULL OR d.status = :status)
              AND (:warehouseId IS NULL OR w.id = :warehouseId)
              AND (:q IS NULL OR LOWER(d.trackingReference) LIKE :q ESCAPE '!'
                   OR LOWER(o.orderNumber) LIKE :q ESCAPE '!' OR LOWER(c.fullName) LIKE :q ESCAPE '!')""")
    Page<Dispatch> search(
            @Param("status") ShipmentStatus status,
            @Param("warehouseId") Long warehouseId,
            @Param("q") String q,
            Pageable pageable);
}
