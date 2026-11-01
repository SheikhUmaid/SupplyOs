package com.supplychain.dispatch;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {

    long countByWarehouseIdAndStatusIn(long warehouseId, Collection<ShipmentStatus> statuses);
}
