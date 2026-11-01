package com.supplychain.tracking;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, Long> {

    @Query("""
            SELECT t FROM DeliveryTracking t
            LEFT JOIN FETCH t.updatedBy
            WHERE t.dispatch.id = :dispatchId
            ORDER BY t.id""")
    List<DeliveryTracking> findHistory(@Param("dispatchId") long dispatchId);
}
