package com.supplychain.allocation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllocationEvaluationRepository extends JpaRepository<AllocationEvaluation, Long> {

    /** Saved best first with rejected warehouses last, so insertion order is display order. */
    @Query("""
            SELECT e FROM AllocationEvaluation e
            JOIN FETCH e.warehouse
            WHERE e.allocation.id = :allocationId
            ORDER BY e.id""")
    List<AllocationEvaluation> findWithWarehouseByAllocationId(@Param("allocationId") long allocationId);
}
