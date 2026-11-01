package com.supplychain.allocation;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseAllocationRepository extends JpaRepository<WarehouseAllocation, Long> {

    long countByOrderId(long orderId);

    /** The highest attempt is the decision currently in force. */
    @Query("""
            SELECT a FROM WarehouseAllocation a
            JOIN FETCH a.order
            LEFT JOIN FETCH a.selectedWarehouse
            WHERE a.order.id = :orderId
            ORDER BY a.attemptNo DESC
            LIMIT 1""")
    Optional<WarehouseAllocation> findLatestByOrderId(@Param("orderId") long orderId);

    @Query(value = """
            SELECT a FROM WarehouseAllocation a
            JOIN FETCH a.order
            LEFT JOIN FETCH a.selectedWarehouse
            WHERE (:outcome IS NULL OR a.outcome = :outcome)""",
            countQuery = """
            SELECT COUNT(a) FROM WarehouseAllocation a
            WHERE (:outcome IS NULL OR a.outcome = :outcome)""")
    Page<WarehouseAllocation> search(@Param("outcome") AllocationOutcome outcome, Pageable pageable);
}
